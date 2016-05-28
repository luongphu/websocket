package websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.Gson;

import entity.WsMessageData;
import entity.WsResponse;

@Service
public class ChatService {
	private Set<WebSocketSession> conns = Collections.synchronizedSet(new HashSet<WebSocketSession>());
	private Map<String, String> nickname_list = new ConcurrentHashMap<String, String>();
	private Map<WebSocketSession, String> session_list = new ConcurrentHashMap<WebSocketSession, String>();
	
	/**
   	* After connection established
   	*/
	public void registerOpenConnection(WebSocketSession session) {
		conns.add(session);
	}
	
	/**
	 * Handle text message
	 */
	public void processMessage(WebSocketSession session, String message) {
		Gson gson = new Gson();
		WsMessageData client_data = gson.fromJson(message, WsMessageData.class);
		int action = client_data.getAction();
		String data = client_data.getData();
		
		// --
		System.out.println(action);
		System.out.println(data);
		// --
		
		switch(action){
			case Constants.JOIN_ROOM:
				OnJoinRoom(session, data);
				break;
			case Constants.SEND_MESSAGE:
				OnSendChatMessage(session, data);
				break;
			case Constants.LEAVE_ROOM:
				OnLeaveRoom(session);
				break;
		}
    }
	
   /**
    * After connection closed
    */
	public void registerCloseConnection(WebSocketSession session) {
		OnLeaveRoom(session);
	}
	
	/**
	 * Check if is logging user
	 * @param session
	 * @return 
	 * 		true 	: is logged user
	 * 		false	: new user
	 */
	private boolean checkIfLoggedUser(WebSocketSession session){
		if(session_list.containsKey(session)){
			return true;
		} else {	
			return false;
		}
	}
	
	/**
	 * 1. Send online list to to login user
	 * 2. Register new user to connection list
	 * 3. Broadcast to everyone know about new comer
	 * @param session 	: login user
	 * @param message	: user name
	 */
	private void OnJoinRoom(WebSocketSession session, String message){
		try {
			Gson gson = new Gson();
			
			// If is logged user,just ignore
			if(checkIfLoggedUser(session) == true){
				return;
			}
			
			// Get online user list
			ArrayList<String> online_list = new ArrayList<String>();
		    for (String session_id : session_list.values()) {
		    	online_list.add(nickname_list.get(session_id));
		    }
		    
		    // Make data
		    WsResponse online_list_response = new WsResponse();
		    online_list_response.setAction(Constants.ONLINE_LIST);
		    online_list_response.setData(online_list);
		    
			String online_list_str = gson.toJson(online_list_response);
			session.sendMessage(new TextMessage(online_list_str));
			
			 // Register new user
			session_list.put(session, session.getId());
			nickname_list.put(session.getId(), message);
			
			// Broadcast to everyone know about new comer
			ArrayList<String> comer_info = new ArrayList<String>();
			comer_info.add(nickname_list.get(session.getId()));
			
			WsResponse comer_info_response = new WsResponse();
			comer_info_response.setAction(Constants.JOIN_ROOM);
			comer_info_response.setData(comer_info);
		    
			String comer_info_response_str = gson.toJson(comer_info_response);
		    for (WebSocketSession sock : conns) {
		    	if(session.getId() != sock.getId()){
		    		sock.sendMessage(new TextMessage(comer_info_response_str));
		    	}
		    }
		    
		} catch (IOException e) {
			System.out.println("IO exception when sending online user list");
		}
	}
	
	/**
	 * Send chat message
	 * @param session
	 * @param message
	 */
	private void OnSendChatMessage(WebSocketSession session, String message){
		try {
			Gson gson = new Gson();
			
			// If new comer user,do nothing
			if(checkIfLoggedUser(session) == false){
				return;
			}
			
			ArrayList<String> chat_info = new ArrayList<String>();
			chat_info.add(nickname_list.get(session.getId()));
			chat_info.add(message);
			
			WsResponse chat_info_response = new WsResponse();
			chat_info_response.setAction(Constants.SEND_MESSAGE);
			chat_info_response.setData(chat_info);
			String chat_info_response_str = gson.toJson(chat_info_response);
			
			// Broadcast the chat message to everyone
	    	for (WebSocketSession sock : conns) {
	    		if(session.getId() != sock.getId()){
	    			sock.sendMessage(new TextMessage(chat_info_response_str));
	    		}
	    	}
		} catch (IOException e) {
			System.out.println("IO exception when sending online user list");
		}
	}
	
	/**
	 * Leave chat room
	 * @param session
	 */
	private void OnLeaveRoom(WebSocketSession session){
		try {
			Gson gson = new Gson();
			
			// Remove this connection from connection list
			conns.remove(session);
			
			// Remove nickname from nickname list
			String session_id = session_list.get(session);
			session_list.remove(session);
			
			// Breadcast to everyone know this user has been left
			if (session_id!= null) {
				ArrayList<String> room_leave_info = new ArrayList<String>();
				room_leave_info.add(session.getId());
				
				WsResponse room_leave_info_response = new WsResponse();
				room_leave_info_response.setAction(Constants.LEAVE_ROOM);
				room_leave_info_response.setData(room_leave_info);
				String chat_info_response_str = gson.toJson(room_leave_info_response);

		    	for (WebSocketSession sock : conns) {
					sock.sendMessage(new TextMessage(chat_info_response_str));
				}
			}
			
		} catch (IOException e) {
			System.out.println("IO exception when sending remove user message");
		}
	}
}
