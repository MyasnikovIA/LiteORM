import orm.Address;
import orm.Student;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        JsonORM orm = new JsonORM(Main.class);
        orm.buildTable(true);
        Student student = new Student();
        student.name = "test";
        student.fio = "555555";
        student.address = new Address();
        student.address.street = "sakdjfhsjkfskjafjkashfjksahdf askdjfhsajkdfjksadh sadjfsakdjf";
        System.out.println(orm.addSql(student));
        List<Object> studentList = orm.getObjectList(Student.class,"");
        System.out.println("((Student)studentList.get(0)).name "+((Student)studentList.get(0)).name);
        System.out.println("((Student)studentList.get(0)).address.street  "+((Student)studentList.get(0)).address.street );
    }
}