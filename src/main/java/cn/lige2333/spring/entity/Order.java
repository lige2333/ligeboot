package cn.lige2333.spring.entity;

public class Order {
    private String id;
    private String value;
    private String isPaid;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getPaid() {
        return isPaid;
    }

    public void setPaid(String paid) {
        isPaid = paid;
    }

    public Order(String id, String value) {
        this.id = id;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", value='" + value + '\'' +
                ", isPaid=" + isPaid +
                '}';
    }
}
