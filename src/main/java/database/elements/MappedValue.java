package database.elements;

import java.io.Serializable;

public class MappedValue implements Comparable<MappedValue>, Serializable {

    public String value = "";
    public double similarityScore = -1;

    public MappedValue(String value) {
        this.value = value;
    }

    public int compareTo(MappedValue mappedValue) {
        if(this.similarityScore > mappedValue.similarityScore) {
            return -1;
        } else if(this.similarityScore < mappedValue.similarityScore){
            return  1;
        }
        return 0;
    }

}
