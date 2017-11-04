package tnel.item;

public class Item {
	public String name = null;
	public int index = 0;
	
	public Item(String name, int index) {
		this.name = name;
		this.index = index;
	}
	
	@Override
	public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        
        String name = null;
        try {
        	name = ((Item) obj).name;
        } catch (Exception e) {
        	return false;
        }
        
        if (name == null) {
        	return false;
        }
        
        return this.name.equalsIgnoreCase(name);
    }
}
