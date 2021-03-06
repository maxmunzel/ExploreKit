package explorekit.operators.GroupByThenOperators;

import explorekit.data.Column;
import explorekit.data.ColumnInfo;
import explorekit.data.Dataset;
import explorekit.data.NumericColumn;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by giladkatz on 05/03/2016.
 */
public class GroupByThenStdev extends GroupByThen {
    private Map<List<Integer>, Double> stdevValuePerKey = new HashMap<>();
    private double missingValuesVal;

    public void processTrainingSet(Dataset dataset, List<ColumnInfo> sourceColumns, List<ColumnInfo> targetColumns) {
        super.processTrainingSet(dataset, sourceColumns, targetColumns);
        Map<List<Integer>, Double> avgValuePerKey = new HashMap<>();

        //start by calculating the averages of every entry
        for (Map.Entry<List<Integer>, Collection<Double>> kv : valuesPerKey.asMap().entrySet()) {
            Collection<Double> values = kv.getValue();
            avgValuePerKey.put(kv.getKey(), values.stream().mapToDouble(Double::doubleValue).average().getAsDouble());
        }

        //now we calculate the standard deviation
        for (Map.Entry<List<Integer>, Collection<Double>> kv : valuesPerKey.asMap().entrySet()) {
            Collection<Double> values = kv.getValue();
            double tempStdev = values.stream().mapToDouble(a -> Math.pow(a-avgValuePerKey.get(kv.getKey()),2)).sum();
            stdevValuePerKey.put(kv.getKey(), Math.sqrt(tempStdev/values.size()));
        }

        //now we compute the "missing values val" - the value for samples in the test set for which we don't have a values based on the training set
        missingValuesVal = stdevValuePerKey.values().stream().mapToDouble(Double::doubleValue).average().getAsDouble();
    }

    /**
     * Generates the values of the new attribute. The values are generated BOTH for the training and test folds
     * (but the values are calculated based ONLY on the training set)
     * @param dataset
     * @param sourceColumns
     * @param targetColumns
     * @return
     */
    public ColumnInfo generate(Dataset dataset, List<ColumnInfo> sourceColumns, List<ColumnInfo> targetColumns, boolean enforceDistinctVal) {
        NumericColumn column = new NumericColumn(dataset.getNumOfInstancesPerColumn());

        int numOfRows = dataset.getNumberOfRows();

        for (int i=0; i<numOfRows; i++) {
            int j = dataset.getIndices().get(i);
            List<Integer> sourceValues = sourceColumns.stream().map(c -> (Integer) c.getColumn().getValue(j)).collect(Collectors.toList());
            if (!stdevValuePerKey.containsKey(sourceValues)) {
                column.setValue(j, missingValuesVal);
            }
            else {
                column.setValue(j, stdevValuePerKey.get(sourceValues));
            }
        }

        //now we generate the name of the new attribute
        String attString = generateName(sourceColumns, targetColumns);
        String finalString = getName().concat(attString).concat(")");

        ColumnInfo newColumnInfo = new ColumnInfo(column, sourceColumns, targetColumns, this.getClass(), finalString);
        if (enforceDistinctVal && !super.isDistinctValEnforced(dataset,newColumnInfo)) {
            return null;
        }
        return newColumnInfo;
    }

    public boolean isApplicable(Dataset dataset, List<ColumnInfo> sourceColumns, List<ColumnInfo> targetColumns) {
        if (super.isApplicable(dataset, sourceColumns, targetColumns)) {
            if (targetColumns.get(0).getColumn().getType().equals(Column.columnType.Numeric)) {
                return true;
            }
        }
        return false;
    }

    public outputType getOutputType() { return outputType.Numeric;}

    public String getName() {
        return "GroupByThenStdev";
    }
}
