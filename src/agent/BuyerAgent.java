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
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import tnel.agent.gui.BuyerGUI;
import tnel.item.BuyerItem;

@SuppressWarnings({"serial"})
public class BuyerAgent extends DutchAgent {
	
	public ConcurrentHashMap<String,ItemBehaviour> items = new ConcurrentHashMap<String,ItemBehaviour>();

	transient protected BuyerGUI buyerGUI;

	public BuyerAgent() {}

	@Override
	protected void setup() {
		super.setup();
		
		behave.addSubBehaviour(new Receiver());

		buyerGUI = new BuyerGUI(this);
		buyerGUI.setVisible(true);
	}

	@Override
	protected void takeDown() {
		super.takeDown();
	}

	@Override
	protected void onGuiEvent(GuiEvent arg0) {
		super.onGuiEvent(arg0);
	}

	public void addItem(String name, int qt, float price, int index) {

		BuyerItem bi = new BuyerItem(name, qt, price, index);
		ItemBehaviour ib = new ItemBehaviour(this, MasterAgent.refresh, bi);
		item_agents.put(name, new ArrayList<AID>());
		items.put(name, ib);

		behave.addSubBehaviour(ib);
	}

	/* BEHAVIOUR */
	public class ItemBehaviour extends TickerBehaviour {

		public BuyerItem bi = null;
		public DFAgentDescription dfd = null;

		public ItemBehaviour(Agent a, long period, BuyerItem bi) {
			super(a, period);
			this.bi = bi;

			ServiceDescription sd = new ServiceDescription();
			sd.setName(bi.name);

			dfd = new DFAgentDescription();
			dfd.addServices(sd);
		}

		public BuyerItem getItem() {
			return this.bi;
		}
		
		public boolean exit() {
			try {
				// inform seller
				ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);

				Iterator<AID> itr = item_agents.get(bi.name).iterator();
				while (itr.hasNext()) {
					msg.addReceiver(itr.next());
				}

				msg.setConversationId(bi.name);
				myAgent.send(msg);

				// update and remove
				buyerGUI.updateItemQtd(bi.current_qt, bi.index);
				buyerGUI.updateItemCpt(MasterAgent.yes, bi.index);
				items.remove(bi.name);
				item_agents.remove(bi.name);

			} catch (Exception e) {
				System.err.println(e.getMessage());
				return false;
			}

			stop();

			return true;
		}

		@Override
		protected void onTick() {
			// check item
			try {
				DFAgentDescription[] result = DFService.search(myAgent, dfd);

				// add sellers for item and subscribe
				for (int i = 0; i < result.length; ++i) {
					AID res_aid = result[i].getName();
					if (!item_agents.get(bi.name).contains(res_aid)) {
						item_agents.get(bi.name).add(res_aid);

						ACLMessage msg = new ACLMessage(ACLMessage.SUBSCRIBE);
						msg.addReceiver(res_aid);
						msg.setConversationId(bi.name);
						send(msg);
					}
				}
			}
			catch (Exception fe) {
				System.err.println(fe.getMessage());
			}
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
					case ACLMessage.AGREE: {

						String name = msg.getConversationId();
						AID aid = msg.getSender();

						System.out.println(myAgent.getName() + " Subscription ok: " + aid + ":item:" + name);

						break;
					}
					case ACLMessage.INFORM: {

						try {
							String name = msg.getConversationId();
							JSONObject content = new JSONObject(msg.getContent());
							float sellerprice = (float) content.getDouble(MasterAgent.price);
							int sellerqt = content.getInt(MasterAgent.quantity);
							int buyers = content.getInt(MasterAgent.buyers);
							AID aid = msg.getSender();
							
							System.out.println(myAgent.getName() + " Inform: " + aid + 
									":item:" + name + ":price:" + sellerprice + ":qt:" + sellerqt);

							BuyerItem bi = items.get(name).getItem();
							
							// price modifier
							if (auto) {
								bi.maxprice = 
										bi.initmaxprice + (bi.initmaxprice * ( (percent * buyers)/100) );
								System.out.println("Buyer Price adjustment: " + bi.maxprice);
							}
							if (sellerprice<=bi.maxprice) {
								// buy it!
								System.out.println(myAgent.getName() + " Buying: " + aid + 
										":item:" + bi.name + ":price:" + bi.maxprice + ":qt:" + bi.wanted_qt);

								ACLMessage reply = msg.createReply();
								reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

								// adjust quantity
								int req_qt=bi.wanted_qt-bi.current_qt;
								if (sellerqt<=req_qt) {
									req_qt = sellerqt;
								}
								
								System.out.println(myAgent.getName() + " request item qt: " + req_qt);

								content = new JSONObject();
								content.put(MasterAgent.price,bi.maxprice);
								content.put(MasterAgent.quantity,req_qt);
								content.put(MasterAgent.buyers,buyers);
								reply.setContent(content.toString());

								myAgent.send(reply);
							}
							buyerGUI.updateItemMax(bi.maxprice, bi.index);
						} catch (Exception e) {
							System.err.println(e.getMessage());
						}
						break;
					}
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
					case ACLMessage.CONFIRM: {
						try {
							String name = msg.getConversationId();
							JSONObject content = new JSONObject(msg.getContent());
							float sellerprice = (float) content.getDouble(MasterAgent.price);
							int sellerqt = content.getInt(MasterAgent.quantity);
							int buyers = content.getInt(MasterAgent.buyers);
							AID aid = msg.getSender();

							System.out.println(myAgent.getName() + " Done: " + aid + 
									":item:" + name + ":price:" + sellerprice + ":qt:" + sellerqt + ":buyers:" + buyers);
							
							print.add("Seller: " + aid.getLocalName() + ", Item: " + name + ", Price: " + sellerprice + ", Quantity: " + sellerqt);

							item_agents.get(name).remove(aid);
							ItemBehaviour bhi = items.get(name);
							BuyerItem bi = bhi.getItem();

							bi.current_qt+=sellerqt;
							if (bi.current_qt>bi.wanted_qt) {
								bi.current_qt=bi.wanted_qt;
							}
							System.out.println(myAgent.getName() + " current item qt: " + bi.current_qt);
							
							buyerGUI.updateItemQtd(bi.current_qt, bi.index);

							if (bi.current_qt>=bi.wanted_qt) {
								bhi.exit();
							}
							
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
			//System.out.println(myAgent.getName() + " : tick for " + bi.name);

			AID agent = null;
			float price = Float.MAX_VALUE;
		
			// check item
			try {
				DFAgentDescription[] result = DFService.search(myAgent, dfd);
				AID[] sellerAgents = new AID[result.length];
				for (int i = 0; i < result.length; ++i) {
					sellerAgents[i] = result[i].getName();
		
					Iterator it = result[i].getAllServices();
					// pick the cheapest
					while (it.hasNext()) {
						ServiceDescription ser = (ServiceDescription) it.next();
						float temp = Float.parseFloat(ser.getType());
						if (temp<price) {
							price = temp;
							agent = result[i].getName();
						}
					}
					//System.out.println(sellerAgents[i].getName());
				}
			}
			catch (Exception fe) {
				System.err.println(fe.getMessage());
			}
		
			// if found
			if (agent!=null) {
				ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				msg.addReceiver(agent);
				msg.setLanguage("English");
				msg.setOntology("Ontology");
				msg.setContent("Buy");
				send(msg);
				stop();
			}

		}
	}
	*/

}
