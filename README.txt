---README---

Run Command:
-gui -agents jade.Boot Main:tnel.agent.MasterAgent

Agents:
- Dutch  -  'Super Agent', all agents extend this class.
- Master -  The Auction organizer, first agent to Boot. 
			Has global auction variables and function for agent creation. 
- Seller -  Agent responsible for auctioning items.
- Buyer  -  Agent that searches and buys items.
