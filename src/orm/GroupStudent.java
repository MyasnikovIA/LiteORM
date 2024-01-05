package orm;

import persistence.Entity;
import persistence.Id;

@Entity
public class GroupStudent {
    @Id
    long id;

    String name;
    Student students;

}
