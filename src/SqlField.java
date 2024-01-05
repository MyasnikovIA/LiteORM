import java.lang.reflect.Field;

public class SqlField {
    public String name="";
    public Class<?> classFild = null;
    public Field field = null;
    public Boolean isAlterTab = false;

    public Boolean isSerialField= false;
    public String sqlType = "";
    public String sqlDefault="";
    public String checkText="";
    public String sizeText="";
    public String isNotNull="";
    public String tabName="";

    public String initSqlField() {
        StringBuilder sbField = new StringBuilder();
        sbField.append(name);
        sbField.append(" ");
        sbField.append(sqlType);
        if (sizeText.length() > 0) {
            sbField.append(" (");
            sbField.append(sizeText);
            sbField.append(") ");
        }
        sbField.append(sizeText);
        sbField.append(" ");
        sbField.append(isNotNull);
        if (sqlDefault.length() > 0) {
            sbField.append(" DEFAULT ");
            sbField.append(sqlDefault);
            sbField.append(" ");
        }
        if (checkText.length() > 0) {
            sbField.append(" CHECK(");
            sbField.append(checkText);
            sbField.append(")");
        }
        sbField.append("\n");
        return sbField.toString();
    }
    public String buildField() {
        StringBuilder sbField = new StringBuilder();
        if (isAlterTab) {
            sbField.append(" ALTER TABLE ");
            sbField.append(tabName.toLowerCase());
            sbField.append(" ADD COLUMN IF NOT EXISTS ");
        }
        sbField.append(name);
        sbField.append(" ");
        sbField.append(sqlType);
        if (sizeText.length() > 0) {
            sbField.append(" (");
            sbField.append(sizeText);
            sbField.append(") ");
        }
        sbField.append(sizeText);
        sbField.append(" ");
        sbField.append(isNotNull);
        if (sqlDefault.length() > 0) {
            sbField.append(" DEFAULT ");
            sbField.append(sqlDefault);
            sbField.append(" ");
        }
        if (checkText.length() > 0) {
            sbField.append(" CHECK(");
            sbField.append(checkText);
            sbField.append(")");
        }
        if (isAlterTab) {
            sbField.append(";\n");
        } else {
            sbField.append("\n");
        }
        return sbField.toString();
    }
}
