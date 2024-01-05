import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SqlTable {
    String name="";
    public Class<?> classTable = null;
    public HashMap<String,SqlField> fieldList = new HashMap<>();
    public SqlField primaryField;
    public String inherits="";
    public List<SqlForeignKey> foreignKey = new ArrayList<>();

}
