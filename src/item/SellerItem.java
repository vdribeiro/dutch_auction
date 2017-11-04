package tnel.item;

public class SellerItem extends Item {
	public int initqt = 0;
	public int currentqt = 0;
	public float initprice = 0;
	public float currentprice = 0;
	public float minprice = 0;
	public float initpricedec = 0;
	public float pricedec = 0;
	public float timedec = 0;
	
	public SellerItem(String name, int qt, float price, float minprice, float pricedec, float timedec, int index) {
		super(name, index);
		this.initqt = qt;
		this.currentqt = qt;
		this.initprice = price;
		this.currentprice = price;
		this.minprice = minprice;
		this.initpricedec = pricedec;
		this.pricedec = pricedec;
		this.timedec = timedec;
	}
	
}
