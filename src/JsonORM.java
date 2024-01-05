import persistence.Entity;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class JsonORM {
    private Class<?> mainClass;
    static HashMap<String, SqlTable> sqlTableList = new HashMap<>();

    private static List<String> classNameShotList = new ArrayList<>();

    static {
        classNameShotList.add("$");
        classNameShotList.add("META-INF");
        classNameShotList.add("org.");
        classNameShotList.add("com.ctc.wstx.osgi");
        classNameShotList.add("net.sf.");
        classNameShotList.add("com.lowagie.bouncycastle.");
        classNameShotList.add("com.lowagie.text.");
        classNameShotList.add("net.sourceforge.barbecue.");
    }

    static Connection conn = null;

    public JsonORM(Class<?> mainClass) {
        this.mainClass = mainClass;
    }

    static public Connection connect() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection("jdbc:postgresql://192.168.15.47:5432/postgres", "USERNAME", "USERPASS");
    }

    static long executeSQL(String SQL) {
        int count = -1;
        try {
            if (conn == null) conn = connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            rs.next();
            count = rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
        return count;
    }

    static List<Hashtable<String, Object>> getTableSQL(String SQL) {
        List<Hashtable<String, Object>> res = new ArrayList<>();
        try {
            if (conn == null) conn = connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            while (rs.next()) {
                Hashtable<String, Object> row = new Hashtable<>();
                for (int index = 1; index <= rs.getMetaData().getColumnCount(); index++) {
                    String columnName = rs.getMetaData().getColumnName(index);
                    Object value = rs.getObject(index);
                    if (value == null) {
                        row.put(columnName, "null");
                    } else {
                        row.put(columnName, value);
                    }
                }
                res.add(row);
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
        return res;
    }

    ///--------------------------------------------------------------------------------------------------------------------
    ///--------------------------------------------------------------------------------------------------------------------
    ///--------------------------------------------------------------------------------------------------------------------
    ///--------------------------------------------------------------------------------------------------------------------
    public boolean buildTable(boolean dropTable) {
        List<Class<?>> res;
        String className = mainClass.getName().replace('.', '/') + ".class";
        String classPath = mainClass.getClassLoader().getResource(className).toString();
        if (classPath.startsWith("jar")) {
            String jarPath = classPath.substring(0, classPath.indexOf('!'));
            // res = getPageJar(jarPath);
        } else {
            // Package pkg = Main.class.getPackage();
            Package pkg = mainClass.getPackage();
            String packageName = pkg.getName();
            res = getBdClasses(packageName,dropTable);
        }
        return true;
    }

    private static void getAnnotationField(Field field, SqlField sqlField) {
        for (Annotation annotationField : field.getAnnotations()) {
            String annotationFieldClass = annotationField.annotationType().getTypeName();
            switch (annotationFieldClass) {
                case "persistence.Check":
                    sqlField.checkText = ((persistence.Check) annotationField).toString();
                    break;
                case "persistence.Size":
                    sqlField.sizeText = ((persistence.Size) annotationField).toString();
                    break;
                case "persistence.NotNull":
                    sqlField.isNotNull = " NOT NULL ";
                    break;
                case "persistence.Default":
                    sqlField.sqlDefault = ((persistence.Default) annotationField).toString();
                    break;
                default:
            }
        }
    }

    public static List<Class<?>> getBdClasses(String packageName,boolean dropTable) {
        //  DROP table IF EXISTS GroupStudent CASCADE;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = null;
        try {
            resources = classLoader.getResources(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<Class<?>> classes = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());
            if (directory.exists()) {
                List<String> classNames = searchFilesClass(directory, directory);
                // --- вынести в отдельную функцию
                // поиск всех классов, которые надо интерпаритировать в БД
                for (String className : classNames) {
                    Class<?> clazz = null;
                    try {
                        clazz = Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        System.err.println("ClassNotFoundException ERROR: " + e);
                    }
                    boolean skipCls = false;
                    for (String fragStr : classNameShotList) {
                        if (className.indexOf(fragStr) != -1) {
                            skipCls = true;
                            break;
                        }
                    }
                    if (skipCls) continue;
                    if (clazz == null) continue;
                    if ((clazz + "").contains("interface"))
                        continue; // обрабатываем только классы , пропускаем интерфейсы
                    Entity anotationBdClass = clazz.getAnnotation(Entity.class);
                    if (anotationBdClass != null) { // если нет анотации WebServerLite.onPage ,тогда пропускаем метод
                        String tableName = "";
                        if (anotationBdClass.name().equals("")) {
                            String onePackageName = clazz.getPackageName();
                            tableName = clazz.getName().substring(onePackageName.length() + 1, clazz.getName().length());
                        } else {
                            tableName = anotationBdClass.name();
                        }
                        SqlTable sqlTable = new SqlTable();
                        sqlTable.name = tableName.toLowerCase();
                        sqlTable.classTable = clazz;
                        sqlTableList.put(tableName.toLowerCase(), sqlTable);
                    }
                }
                // Перебираем все
                for (Map.Entry<String, SqlTable> set : sqlTableList.entrySet()) {
                    SqlTable sqlTable = set.getValue();
                    Class<?> clazz = sqlTable.classTable;
                    String tableName = set.getKey();
                    // создаем ключевые поля в таблице
                    for (Field field : clazz.getDeclaredFields()) {
                        persistence.Id anotation = (persistence.Id) field.getAnnotation(persistence.Id.class);
                        if (anotation != null) {
                            SqlField sqlField = new SqlField();
                            getAnnotationField(field, sqlField);
                            sqlField.field = field;
                            sqlField.name = field.getName().toLowerCase();
                            //sqlField.typeProp = " SERIAL PRIMARY KEY ";
                            sqlField.sqlType = " SERIAL PRIMARY KEY ";
                            sqlField.isSerialField = true;
                            sqlField.isAlterTab = false;
                            sqlField.classFild = field.getType();
                            sqlTable.fieldList.put(sqlField.name, sqlField);
                            sqlTable.primaryField = sqlField;
                        }
                    }
                    // создаем остальные поля в таблице
                    for (Field field : clazz.getDeclaredFields()) {
                        String nameFieldTab = field.getName().toLowerCase();
                        if (sqlTable.fieldList.containsKey(nameFieldTab)) continue;
                        SqlField sqlField = new SqlField();
                        sqlField.name = nameFieldTab;
                        sqlField.tabName = tableName;
                        sqlField.field = field;
                        sqlField.isSerialField = false;
                        sqlField.isAlterTab = true;
                        sqlField.classFild = field.getType();
                        if (sqlField.sqlType.length() == 0) {
                            sqlField.sqlType = getPgFieldSmolType(sqlTable, field.getType().getTypeName(), sqlField);
                        }
                        sqlTable.fieldList.put(sqlField.name, sqlField);
                    }
                    // ссылка на родительский класс
                    if (!"Object".equals(clazz.getSuperclass().getSimpleName())) {
                        sqlTable.inherits = clazz.getSuperclass().getSimpleName().toLowerCase();
                    }
                    for (Method method : clazz.getMethods()) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                    }
                    sqlTableList.put(sqlTable.name, sqlTable);
                    // System.out.println("sqlTable.fieldList "+sqlTable.fieldList);
                }
            }
        }
        StringBuilder sqlAll = new StringBuilder();
        StringBuilder alterSql = new StringBuilder();
        StringBuilder alterINHERITSql = new StringBuilder();
        StringBuilder alterFOREIGN_KEYSql = new StringBuilder();
        if (dropTable) {
            for (Map.Entry<String, SqlTable> set : sqlTableList.entrySet()) {
                SqlTable table = set.getValue();
                sqlAll.append("DROP TABLE IF EXISTS ");
                sqlAll.append(table.name);
                sqlAll.append(" CASCADE; \n");
            }
        }

        //------------------------------------------------
        // https://postgrespro.ru/docs/postgresql/9.4/sql-comment
        // COMMENT ON TABLE mytable IS 'Это моя таблица.';
        // Удаления комментария: COMMENT ON TABLE mytable IS NULL;
        //------------------------------------------------

        for (Map.Entry<String, SqlTable> set : sqlTableList.entrySet()) {
            SqlTable table = set.getValue();
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE IF NOT EXISTS ");
            sb.append(table.name);
            sb.append(" (\n");
            int countFild = 0;
            for (Map.Entry<String, SqlField> setField : table.fieldList.entrySet()) {
                SqlField field = setField.getValue();
                if ((!field.isAlterTab) || (field.isSerialField)) {
                    countFild++;
                    if (countFild > 1) {
                        sb.append(",");
                    }
                    sb.append(field.initSqlField());
                } else {
                    alterSql.append(field.buildField());
                }
            }
            sb.append("\n)");
            if (table.inherits.length() > 0) {
                alterINHERITSql.append("ALTER TABLE ");
                alterINHERITSql.append(table.name.toLowerCase());
                alterINHERITSql.append(" INHERIT ");
                alterINHERITSql.append(table.inherits);
                alterINHERITSql.append("; \n");
            }
            sb.append(";\n");
            sqlAll.append(sb);
        }

        // ALTER TABLE orders ADD CONSTRAINT fk_orders_customers FOREIGN KEY (customer_id) REFERENCES customers (id);
        for (Map.Entry<String, SqlTable> set : sqlTableList.entrySet()) {
            SqlTable table = set.getValue();
            for (SqlForeignKey foreignKey : table.foreignKey) {
                alterFOREIGN_KEYSql.append(" ALTER TABLE ");
                alterFOREIGN_KEYSql.append(foreignKey.srcTable);
                alterFOREIGN_KEYSql.append(" ADD CONSTRAINT ");
                alterFOREIGN_KEYSql.append(foreignKey.name);
                alterFOREIGN_KEYSql.append(" FOREIGN KEY (");
                alterFOREIGN_KEYSql.append(foreignKey.srcProperty);
                alterFOREIGN_KEYSql.append(") ");
                alterFOREIGN_KEYSql.append(" REFERENCES ");
                alterFOREIGN_KEYSql.append(foreignKey.dstTable);
                alterFOREIGN_KEYSql.append(" (");
                alterFOREIGN_KEYSql.append(foreignKey.dstProperty);
                alterFOREIGN_KEYSql.append(") ");
                alterFOREIGN_KEYSql.append(foreignKey.actions);
                alterFOREIGN_KEYSql.append(";\n");
            }
        }
        sqlAll.append(alterINHERITSql);
        sqlAll.append(alterSql);
        sqlAll.append(alterFOREIGN_KEYSql);
        executeSQL(sqlAll.toString());
        return classes;
    }
    /**
     * Получение списка классов в директории
     *
     * @param directory
     * @param startDir
     * @return
     */
    public static List<String> searchFilesClass(File directory, File startDir) {
        List<String> strClassesList = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    strClassesList.addAll(searchFilesClass(file, startDir)); // recursively search subdirectories
                } else {
                    String fileStr = (file.getAbsolutePath().substring(startDir.getAbsolutePath().length() + 1)).replace("\\", ".");
                    if (fileStr.substring(fileStr.length() - 6).equals(".class")) {
                        fileStr = fileStr.substring(0, fileStr.length() - 6);
                        strClassesList.add(fileStr);
                    }
                }
            }
        }
        return strClassesList;
    }
    private static String getPgFieldSmolType(SqlTable sqlTable, String typePropName, SqlField sqlField) {
        // https://www.instaclustr.com/blog/postgresql-data-types-mappings-to-sql-jdbc-and-java-data-types/
        switch (typePropName) {
            case "int":
                typePropName = "INTEGER";
                break;
            case "java.lang.Float":
                typePropName = " numeric ";
                break;
            case "boolean":
                typePropName = " bool ";
                break;
            case "double":
                typePropName = " real ";
                break;
            case "java.sql.Date":
                typePropName = " date ";
                break;
            case "java.sql.Time":
                typePropName = " timestamp ";
                break;
            case "java.sql.SQLXML":
                typePropName = " xml ";
                break;
            case "java.sql.Timestamp":
                typePropName = " time ";
                break;
            case "float":
                typePropName = " float4 ";
                break;
            case "java.math.BigDecimal": // для денег использовать "numeric"
                // https://ru.stackoverflow.com/questions/667706/%D0%92-%D1%87%D0%B5%D0%BC-%D1%85%D1%80%D0%B0%D0%BD%D0%B8%D1%82%D1%8C-%D0%B4%D0%B5%D0%BD%D1%8C%D0%B3%D0%B8-float-double
                typePropName = " numeric ";
                break;
            case "java.lang.String":
                typePropName = "VARCHAR";
                break;
            case "long":
                typePropName = "BIGINT";
                break;
            default:
                sqlField.isAlterTab = true;
                String smolFieldName = typePropName.substring(typePropName.lastIndexOf(".") + 1, typePropName.length()).toLowerCase();
                if (sqlTableList.containsKey(smolFieldName)) { // если ссылка на другую таблицу, тогда создаем вторичный ключ
                    // https://metanit.com/sql/postgresql/2.5.php
                    SqlTable sqlTableChild = sqlTableList.get(smolFieldName);
                    typePropName = "BIGINT";
                    if (sqlTableChild.primaryField !=null) {
                        SqlForeignKey sqlForeignKey = new SqlForeignKey();
                        sqlForeignKey.name = "fk_" + sqlTable.name + "_" + sqlField.name.toLowerCase();
                        sqlForeignKey.srcProperty = sqlField.name.toLowerCase();
                        sqlForeignKey.srcTable = sqlTable.name;
                        sqlForeignKey.dstProperty = sqlTableChild.primaryField.name;
                        sqlForeignKey.dstTable = sqlTableChild.name;
                        sqlTable.foreignKey.add(sqlForeignKey);
                        // System.out.println("FOREIGN KEY ( " + sqlField.name.toLowerCase() + ") REFERENCES " + sqlTableChild.name + " (" + sqlTableChild.primaryKey + ")");
                        // ALTER TABLE orders ADD CONSTRAINT fk_orders_customers FOREIGN KEY (customer_id) REFERENCES customers (id);
                    }
                }
        }
        sqlField.sqlType = typePropName;
        return typePropName;
    }
    public String addSql(Object objectSave) {
        Class<?> clazz = objectSave.getClass();
        String tabName = clazz.getSimpleName().toLowerCase();
        if (!sqlTableList.containsKey(tabName)) return "null";
        SqlTable sqlTable = sqlTableList.get(tabName);
        StringBuilder fieldsName = new StringBuilder();
        StringBuilder fieldsVal = new StringBuilder();
        StringBuilder fieldsNameVal = new StringBuilder();
        Object valueId = null;
        try {
            if (sqlTable.primaryField != null) {
                valueId = sqlTable.primaryField.field.get(objectSave);
            }
        } catch (IllegalAccessException e) {
            System.err.println(e.getMessage());
        }
        for (Map.Entry<String, SqlField> set : sqlTable.fieldList.entrySet()) {
            SqlField sqlField = set.getValue();
            String nameField = sqlField.field.getName().toLowerCase();
            if (nameField.equals(sqlTable.primaryField.name)) continue; // пропускаем основной ключ
            String typeField = sqlField.field.getType().getSimpleName().toLowerCase();
            String typeValue = sqlField.sqlType.toLowerCase();
            Object value = null;
            try {
                value = sqlField.field.get(objectSave);
            } catch (IllegalAccessException e) {
                System.err.println(e.getMessage());
            }
            if ((typeValue.indexOf("serial") != -1) && (String.valueOf(value).equals("0"))) {
                continue;
            }
            if (fieldsName.toString().length() > 0) {
                fieldsName.append(",");
                fieldsVal.append(",");
            }
            if (sqlTableList.containsKey(typeField)) {
                fieldsName.append(nameField);
                if (value != null) {
                    fieldsVal.append(" ");
                    fieldsVal.append(addSql(value));
                } else {
                    fieldsVal.append("null");
                }
            } else {
                if (value == null) {
                    fieldsName.append(nameField);
                    fieldsVal.append("null");
                    fieldsNameVal.append(nameField);
                    fieldsNameVal.append("=null");
                } else if ((typeValue.indexOf("bool") != -1)
                        || (typeValue.indexOf("bigserial") != -1)
                        || (typeValue.indexOf("int8") != -1)
                        || (typeValue.indexOf("oid") != -1)
                        || (typeValue.indexOf("numeric") != -1)
                        || (typeValue.indexOf("int4") != -1)
                        || (typeValue.indexOf("serial") != -1)
                        || (typeValue.indexOf("float4") != -1)
                        || (typeValue.indexOf("float8") != -1)
                        || (typeValue.indexOf("int2") != -1)
                        || (typeValue.indexOf("money") != -1)
                        || (typeValue.indexOf("smallserial") != -1)
                        || (typeValue.indexOf("bit") != -1)) {
                    fieldsName.append(nameField);
                    fieldsVal.append(value);
                    if (fieldsNameVal.toString().length() > 0) {
                        fieldsNameVal.append(",");
                    }
                    fieldsNameVal.append(nameField);
                    fieldsNameVal.append("=");
                    fieldsNameVal.append(value);
                } else {
                    fieldsName.append(nameField);
                    fieldsVal.append("'");
                    fieldsVal.append(value);
                    fieldsVal.append("' ");
                    if (fieldsNameVal.toString().length() > 0) {
                        fieldsNameVal.append(",");
                    }
                    fieldsNameVal.append(nameField);
                    fieldsNameVal.append("='");
                    fieldsNameVal.append(value);
                    fieldsNameVal.append("' ");
                }
            }
        }
        StringBuilder SQL = new StringBuilder();
        if (String.valueOf(valueId).equals("0")) {
            SQL.append("insert into ");
            SQL.append(tabName);
            SQL.append(" (");
            SQL.append(fieldsName);
            SQL.append(") values (");
            SQL.append(fieldsVal);
            SQL.append(") RETURNING ");
            SQL.append(sqlTable.primaryField.name);
        } else {
            SQL.append("UPDATE ");
            SQL.append(tabName);
            SQL.append(" SET ");
            SQL.append(fieldsNameVal);
            SQL.append(" WHERE ");
            SQL.append(sqlTable.primaryField.name);
            SQL.append(" = ");
            SQL.append(valueId);
            SQL.append(" RETURNING ");
            SQL.append(sqlTable.primaryField.name);
        }
        long newId = executeSQL(SQL.toString());
        if (newId != -1) {
            if (sqlTable.primaryField != null) {
                try {
                    sqlTable.primaryField.field.set(objectSave, newId);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return String.valueOf(newId);
    }
    public List<Object> getObjectList(Class<?> objectClass, String sqlWhere) {
        List<Object> res = new ArrayList<>();
        String tabName = objectClass.getSimpleName().toLowerCase();
        if (!sqlTableList.containsKey(tabName)) return res;
        SqlTable sqlTable = sqlTableList.get(tabName);
        Class<?> tabClass = sqlTable.classTable;
        // System.out.println(tabName);
        // System.out.println(tabClass);
        StringBuilder SQL = new StringBuilder();
        SQL.append(" select * from ");
        SQL.append(tabName);
        if (sqlWhere.length() > 0) {
            SQL.append(" where ");
            SQL.append(sqlWhere);
        }
        List<Hashtable<String, Object>> rows = getTableSQL(SQL.toString());
        for (Hashtable<String, Object> row : rows) {
            try {
                Object objectTabClass = tabClass.newInstance();
                for (Map.Entry<String, Object> set : row.entrySet()) {
                    Object obj = set.getValue();
                    String nam = set.getKey();
                    if (sqlTable.fieldList.containsKey(nam)) {
                        SqlField sqlField = sqlTable.fieldList.get(nam);
                        String simpleFieldName = sqlField.field.getType().getSimpleName().toLowerCase();
                        if (sqlTableList.containsKey(simpleFieldName)) {
                            if (obj == null || "null".equals(obj)) {
                                sqlField.field.set(objectTabClass, null);
                            } else {
                                SqlTable subTab =  sqlTableList.get(simpleFieldName);
                                // sqlField.field.getType()
                                List<Object> studentList = getObjectList(sqlField.field.getType(),subTab.primaryField.name+" = "+obj);
                                if (studentList.size()>0) {
                                    sqlField.field.set(objectTabClass, studentList.get(0));
                                } else {
                                    sqlField.field.set(objectTabClass, null);
                                }
                            }
                        } else {
                            sqlField.field.set(objectTabClass, obj);
                        }
                    }
                }
                res.add(objectTabClass);
            } catch (InstantiationException e) {
                System.err.println(e.getMessage());
            } catch (IllegalAccessException e) {
                System.err.println(e.getMessage());
            }
        }
        return res;
    }

}
