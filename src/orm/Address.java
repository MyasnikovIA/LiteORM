package orm;

import persistence.Entity;
import persistence.Id;

@Entity
public class Address{
    @Id
    public long Address_id;
    public String street;

    public Geo_point gps;
}

