/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tnel.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import tnel.agent.gui.SellerGUI;
import tnel.item.SellerItem;

@SuppressWarnings({"serial"})
public class SellerAgent extends DutchAgent {

	public ConcurrentHashMap<String,ItemBehaviour> items = new ConcurrentHashMap<String,ItemBehaviour>();
	public DFAgentDescription dfd = null;

	transient protected SellerGUI sellerGUI;

	public SellerAgent() {}

	@Override
	protected void setup() {
		super.setup();

		// Register in the yellow pages
		dfd = new DFAgentDescription();
		dfd.setName(getAID());

		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			System.err.println(fe.getMessage());
		}

		behave.addSubBehaviour(new Receiver());

		sellerGUI = new SellerGUI(this);
		sellerGUI.setVisible(true);

	}

	@Override
	protected void takeDown() {

		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			System.err.println(fe.getMessage());
		}

		super.takeDown();
	}

	@Override
	protected void onGuiEvent(GuiEvent arg0) {
		super.onGuiEvent(arg0);
	}

	public void addItem(String name, int qt, float price, float minprice, float pricedec, float timedec, int index) {

		// register item
		ServiceDescription sd = new ServiceDescription();
		try {
			sd.setName(name);
			sd.setType(Float.toString(price));
			sd.setOwnership(getAID().getName());

			dfd.addServices(sd);
			DFService.modify(this, dfd);
		}
		catch (Exception fe) {
			System.err.println(fe.getMessage());
		}

		SellerItem si = new SellerItem(name, qt, price, minprice, pricedec, timedec, index);
		ItemBehaviour ib = new ItemBehaviour(this, (long) timedec * 1000, si, sd);
		item_agents.put(name, new ArrayList<AID>());
		items.put(name, ib);

		behave.addSubBehaviour(ib);
	}

	/* BEHAVIOUR */

	public class ItemBehaviour extends TickerBehaviour {

		SellerItem si = null;
		ServiceDescription sd = null;

		public ItemBehaviour(Agent a, long period, SellerItem si, ServiceDescription sd) {
			super(a, period);
			this.si = si;
			this.sd = sd;
		}

		public SellerItem getItem() {
			return this.si;
		}

		public boolean exit() {
			try {
				dfd.removeServices(sd);
				DFService.modify(myAgent, dfd);

				// inform buyers
				ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);

				Iterator<AID> itr = item_agents.get(si.name).iterator();
				while (itr.hasNext()) {
					msg.addReceiver(itr.next());
				}

				msg.setConversationId(si.name);
				JSONObject content = new JSONObject();
				content.put(MasterAgent.price,si.currentprice);
				content.put(MasterAgent.quantity,si.currentqt);
				msg.setContent(content.toString());
				myAgent.send(msg);

				// update and remove
				int buyers = getAgentCount(si.name);
				sellerGUI.updateItemPrc(si.currentprice, si.index);
				sellerGUI.updateItemQtd(si.initqt-si.currentqt, si.index);
				sellerGUI.updateItemBuy(buyers, si.index);
				sellerGUI.updateItemCpt(MasterAgent.yes, si.index);
				items.remove(si.name);
				item_agents.remove(si.name);

			} catch (Exception e) {
				System.err.println(e.getMessage());
				return false;
			}

			stop();

			return true;
		}

		@Override
		protected void onTick() {
			//System.out.println(myAgent.getName() + " : tick for " + si.name);
			int buyers = getAgentCount(si.name);
			// price modifier
			if (auto) {
				si.pricedec = 
						si.initpricedec - (si.initpricedec * ( (percent * buyers)/100) );
				if (si.pricedec<1) si.pricedec=1;
				System.out.println("Seller Price adjustment: " + si.pricedec);
			}
			if (si.currentprice>si.minprice+si.pricedec) {
				si.currentprice-=si.pricedec;
			} else if (si.currentprice>si.minprice) {
				si.currentprice=si.minprice;
			} else {
				// close auction and remove item
				exit();
				return;
			}

			// Inform interested
			try {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

				Iterator<AID> itr = item_agents.get(si.name).iterator();
				while (itr.hasNext()) {
					msg.addReceiver(itr.next());
				}

				msg.setConversationId(si.name);
				JSONObject content = new JSONObject();
				content.put(MasterAgent.price, si.currentprice);
				content.put(MasterAgent.quantity,si.currentqt);
				content.put(MasterAgent.buyers,getAgentCount(si.name));
				msg.setContent(content.toString());
				myAgent.send(msg);

				sellerGUI.updateItemPrc(si.currentprice, si.index);
				sellerGUI.updateItemQtd(si.initqt-si.currentqt, si.index);
				sellerGUI.updateItemBuy(buyers, si.index);

			} catch (Exception e) {
				System.err.println(e.getMessage());
			}

			/*
			// modify item
			try {
				dfd.removeServices(sd);
				sd.setType(Float.toString(si.price));
				dfd.addServices(sd);
				DFService.modify(myAgent, dfd);

				sellerGUI.updateItem(si.price, si.index);
			}
			catch (Exception fe) {
				System.err.println(fe.getMessage());
			}
			 */

		}

	}

	public class Receiver extends CyclicBehaviour {

		@Override
		public void action() {
			try {
				ACLMessage msg = myAgent.receive();
				if (msg != null) {
					// Message received. Process it
					int flag = msg.getPerformative();

					switch (flag) {
					case ACLMessage.CANCEL: {

						String name = msg.getConversationId();
						AID aid = msg.getSender();

						System.out.println(myAgent.getName() + " Cancel: " + aid + ":item:" + name);

						try {
							item_agents.get(name).remove(aid);
						} catch (Exception e) {
							System.err.println(e.getMessage());
						}
						break;
					}
					case ACLMessage.SUBSCRIBE: {
						try {
							String name = msg.getConversationId();
							AID aid = msg.getSender();
							item_agents.get(name).add(aid);

							System.out.println(myAgent.getName() + " Subscribe request: " + aid + ":item:" + name);

							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACLMessage.AGREE);
							myAgent.send(reply);
						} catch (Exception e) {
							System.err.println(e.getMessage());
						}

						break;
					}
					case ACLMessage.ACCEPT_PROPOSAL: {
						try {
							String name = msg.getConversationId();
							JSONObject content = new JSONObject(msg.getContent());
							float buyerprice = (float) content.getDouble(MasterAgent.price);
							int buyerqt = content.getInt(MasterAgent.quantity);
							int buyers = content.getInt(MasterAgent.buyers);
							AID aid = msg.getSender();

							System.out.println(myAgent.getName() + " Buy request: " + aid + 
									":item:" + name + ":price:" + buyerprice + ":qt:" + buyerqt);

							print.add("Buyer: " + aid.getLocalName() + ", Item: " + name + ", Price: " + buyerprice + ", Quantity: " + buyerqt);

							item_agents.get(name).remove(aid);
							ItemBehaviour bhi = items.get(name);
							SellerItem si = bhi.getItem();

							if (si.currentqt<buyerqt) {
								buyerqt=si.currentqt;
							}
							
							si.currentqt-=buyerqt;
							System.out.println(myAgent.getName() + " current item qt: " + si.currentqt);

							sellerGUI.updateItemPrc(si.currentprice, si.index);
							sellerGUI.updateItemQtd(si.initqt-si.currentqt, si.index);

							if (si.currentqt<=0) {
								bhi.exit();
							}

							/*
							int qt = buyerqt;
							if (si.qt<=buyerqt) {
								qt=si.qt;
								si.qt-=qt;
								bhi.exit();
							} else {
								si.qt-=qt;
							}
							 */

							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACLMessage.CONFIRM);
							content = new JSONObject();
							content.put(MasterAgent.price,buyerprice);
							content.put(MasterAgent.quantity,buyerqt);
							content.put(MasterAgent.buyers,buyers);
							reply.setContent(content.toString());
							myAgent.send(reply);

						} catch (Exception e) {
							System.err.println(e.getMessage());
						}

						break;
					}
					default:
						break;
					}
				}
				else {
					block();
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}

	/*
	public class Sender extends CyclicBehaviour {

		@Override
		public void action() {
			for (Entry<SellerItem, ArrayList<AID>> entry : interested.entrySet()) {
				SellerItem key = entry.getKey();
				ArrayList<AID> value = entry.getValue();

				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

				Iterator<AID> itr = value.iterator();
				while (itr.hasNext()) {
					msg.addReceiver(itr.next());
				}

				msg.setConversationId(key.name);
				msg.setContent(Float.toString(key.price));
				send(msg);
			}

		}
	}
	 */

}
