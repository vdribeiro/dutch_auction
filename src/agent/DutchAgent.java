package tnel.agent;

import jade.core.AID;
import jade.core.behaviours.ParallelBehaviour;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/** THE MASTER AGENT */

@SuppressWarnings({"serial","rawtypes"})
public class DutchAgent extends GuiAgent {

	public int id = 0;
	public String name = "Agent";
	public boolean auto = false;
	public int percent = 0;
	public ArrayList<String> print = new ArrayList<String>();
	public ConcurrentHashMap<String,ArrayList<AID>> item_agents = new ConcurrentHashMap<String,ArrayList<AID>>();

	public ParallelBehaviour behave = null;

	public DutchAgent() {}

	@Override
	protected void setup() {
		super.setup();

		try {
			Object[] args = getArguments();
			id = (Integer) args[0];
			name = (String) args[1];
			auto =  (Boolean) args[2];
			percent =  (Integer) args[3];
			
		} catch (Exception e) {
			//System.err.println(e.getMessage());
		}

		try {
			System.out.println("Hap : " + getAID().getHap());
			System.out.println("Local Name : " + getAID().getLocalName());
			System.out.println("Name : " + getAID().getName());

			System.out.println("Agent Id : " + id);
			System.out.println("Agent Name : " + name);
			System.out.println("Agent Auto : " + auto);
			System.out.println("Agent Percent : " + percent);

			Iterator it = getAID().getAllAddresses();
			System.out.println("Adresses : ");
			while (it.hasNext()) {
				System.out.println(it.next());
			}

			behave = new ParallelBehaviour(this,ParallelBehaviour.WHEN_ALL);
			addBehaviour(behave);

		} catch (Exception e) {
			//System.err.println(e.getMessage());
		}
	}

	@Override
	protected void takeDown() {
		System.out.println("Terminating : " + getAID().getName());
		super.takeDown();
	}

	@Override
	protected void onGuiEvent(GuiEvent arg0) {
		System.out.println(arg0);
	}

	public ArrayList<String> transactions() {
		return print;
	}

	public int getAgentCount(String item) {
		int count = 0;

		try {
			count = item_agents.get(item).size();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		return count;
	}
}
