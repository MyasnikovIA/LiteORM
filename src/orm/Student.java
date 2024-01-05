package orm;

import persistence.Entity;
import persistence.Id;

@Entity
public class Student {
    @Id
    public long Student_id;
    public String name;
    public String fio;
    public Address address;
}

