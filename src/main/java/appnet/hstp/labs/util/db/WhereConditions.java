package appnet.hstp.labs.util.db;

import java.util.ArrayList;
import java.util.List;

public class WhereConditions {
    protected final List<ComparisonOperator> conditions = new ArrayList<>();
    protected final List<String> logicalOperators = new ArrayList<>();

    public List<ComparisonOperator> getConditions() {
        return conditions;
    }

    public List<String> getLogicalOperators() {
        return logicalOperators;
    }

    public WhereConditions and(ComparisonOperator operator) {
        conditions.add(operator);
        logicalOperators.add("AND");
        return this;
    }

    public WhereConditions or(ComparisonOperator operator) {
        conditions.add(operator);
        logicalOperators.add("OR");
        return this;
    }

    public WhereConditions and(WhereConditions other) {
        conditions.addAll(other.conditions);
        logicalOperators.add("AND");
        logicalOperators.addAll(other.logicalOperators);
        return this;
    }

    public WhereConditions or(WhereConditions other) {
        conditions.addAll(other.conditions);
        logicalOperators.add("OR");
        logicalOperators.addAll(other.logicalOperators);
        return this;
    }
}