package orm;

import persistence.Entity;
import persistence.Id;

@Entity
public class Geo_point {

    @Id
    long id;
    private double latitude ;
    private double longitude ;
    private double radius=0 ;
}

