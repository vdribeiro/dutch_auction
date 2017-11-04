package tnel.item;

public class BuyerItem extends Item {
	public int wanted_qt = 0;
	public int current_qt = 0;
	public float initmaxprice = 0;
	public float maxprice = 0;
	
	public BuyerItem(String name, int wqt, float price, int index) {
		super(name, index);
		this.wanted_qt = wqt;
		this.current_qt = 0;
		this.initmaxprice = price;
		this.maxprice = price;
	}
	
}
