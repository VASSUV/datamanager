package ru.mediasoft.datamanager;

public class Column {
    public String name;
    public String type;
    public boolean isNotNull;
    public String defaultValue ;
    public boolean isPrimaryKey;

    public Column (String name, String type, boolean isNotNull, String defaultValue, boolean isPrimaryKey) {
        this.name = name;
        this.type = type;
        this.isNotNull = isNotNull;
        this.defaultValue = defaultValue;
        this.isPrimaryKey = isPrimaryKey;
    }
}
