public class SqlForeignKey {

    public String name="";

    public String srcTable="";

    public String srcProperty="";
    public String dstTable="";
    public String dstProperty="";

    /**
     * С помощью выражений ON DELETE и ON UPDATE можно установить действия, которые выполняются соответственно при удалении и изменении связанной строки из главной таблицы. Для установки подобного действия можно использовать следующие опции:
     *
     * CASCADE: автоматически удаляет или изменяет строки из зависимой таблицы при удалении или изменении связанных строк в главной таблице.
     *
     * RESTRICT: предотвращает какие-либо действия в зависимой таблице при удалении или изменении связанных строк в главной таблице. То есть фактически какие-либо действия отсутствуют.
     *
     * NO ACTION: действие по умолчанию, предотвращает какие-либо действия в зависимой таблице при удалении или изменении связанных строк в главной таблице. И генерирует ошибку. В отличие от RESTRICT выполняет отложенную проверку на связанность между таблицами.
     *
     * SET NULL: при удалении связанной строки из главной таблицы устанавливает для столбца внешнего ключа значение NULL.
     *
     * SET DEFAULT: при удалении связанной строки из главной таблицы устанавливает для столбца внешнего ключа значение по умолчанию, которое задается с помощью атрибуты DEFAULT. Если для столбца не задано значение по умолчанию, то в качестве него применяется значение NULL.
     *
     *     FOREIGN KEY (CustomerId) REFERENCES Customers (Id) ON DELETE SET NULL
     *     FOREIGN KEY (CustomerId) REFERENCES Customers (Id) ON DELETE SET DEFAULT
     *     FOREIGN KEY (CustomerId) REFERENCES Customers (Id) ON DELETE CASCADE
     *     FOREIGN KEY (CustomerId) REFERENCES Customers (Id) ON DELETE SET DEFAULT
     */
    public String actions="";


}
