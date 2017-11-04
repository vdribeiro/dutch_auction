package tnel.agent;

import jade.gui.GuiEvent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.util.ArrayList;

import javax.swing.UnsupportedLookAndFeelException;

import tnel.gui.MainMenu;

/** THE MASTER AGENT */

@SuppressWarnings("serial")
public class MasterAgent extends DutchAgent {
	
	/* BEHAVIOURS */
	public static int timeout = 10000;
	public static int refresh = 1000;

	/* AUCTION TAGS */
	public static final String buyers = "buyers";
	public static final String quantity = "quantity";
	public static final String price = "price";
	
	/* MESSAGES */
	public static final String language = "English";
	public static final String Ontology = "Ontology";

	public static final String yes = "Yes";
	public static final String no = "No";

	/* AGENTS */
	//public static String pack = getClass().getPackage() + ".";

	public static String master = MasterAgent.class.getName();
	public static String buyer = BuyerAgent.class.getName();
	public static String seller = SellerAgent.class.getName();
	
	public MasterAgent() {
		nimbus();
	}

	@Override
	protected void setup() {
		super.setup();
		
		GUI = new MainMenu(this);
		GUI.setVisible(true);
	}

	@Override
	protected void takeDown() {
		super.takeDown();
	}

	@Override
	protected void onGuiEvent(GuiEvent arg0) {
		super.onGuiEvent(arg0);
	}

	public void createAgent(String name, String type, ArrayList<Object> agentParameters){

		//this.postGuiEvent(null);

		AgentContainer agentContainer = getContainerController();	

		//String agentID = new AID(name, AID.ISLOCALNAME).getLocalName();

		AgentController controller = null;
		try {
			controller = agentContainer.createNewAgent(name, type, agentParameters.toArray());
			controller.start();
		} catch (Exception e) {
			System.err.println("Problem creating new agent: " + e.getMessage());
		}
	}

	/* GUI */
	transient protected MainMenu GUI;

	public boolean nimbus() {
		/*
		 * Set the Nimbus look and feel
		 */
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		/*
		 * If Nimbus (introduced in Java SE 6) is not available, stay with the
		 * default look and feel. For details see
		 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
		 */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(MainMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
			return false;
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(MainMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
			return false;
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(MainMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
			return false;
		} catch (UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(MainMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
			return false;
		} catch (Exception ex) {
			java.util.logging.Logger.getLogger(MainMenu.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
			return false;
		}
		//</editor-fold>


		return true;
	}

}
