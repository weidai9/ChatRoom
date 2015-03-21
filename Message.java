public class Message {
	/*
	 * different types of message:
	 * In string format: [type:xx] [data: [data1], [data2]]
	 * The first two characters are type of message
	 * Then follows a space
	 * The followsing content is the protocal format:
	 * 	- 99: login request
	 * 	- 00: login username data:
	 * 		- "00 [username]"
	 * 	- 01: login password data:
	 * 		- "01 [password]"
	 * 	- 02: login return type:
	 * 		- "02 username_ok": user name is valid
	 * 		- "02 username_error": user name not exists
	 * 		- "02 password_ok": password is correct
	 * 		- "02 password_error": password is invalid
	 * 		- "02 server_blocked": after limited times of try, client get blocked
	 * 		- "02 invalid_session": the username is used on another client and invalid on this ip
	 * 	- 03: user's receive socket port
	 * 		- "03 [port number]"
	 * 	- 04: Presence Broadcasts:
	 * 		- "04 [broadcast data]"
	 *  - 05: Message through server
	 *  	- "05 [from] [to] [data]"
	 *  - 06: Response from server
	 *  	- "06 OK": successfully sent
	 *  	- "06 USERNAME_ERROR"
	 *  	- "06 TARGET_OFFLINE"
	 *  	- "06 INVALID_SESSION"
	 *  	- "06 TIME_OUT"
	 *  	- "06 MESSAGE_BLOCKED"
	 *  	- "06 UNBLOCK_FAIL"
	 *  	- "06 NO_REQUEST"
	 *  - 07: Heart Beat
	 *  	- "07 [Username]"
	 *  - 08: Block and unblock another user
	 *  	- "08 [from] [target] block/unblock"
	 *  - 09: logout:
	 *  	- "09 [username]"
	 *  - 10: get online users' list
	 *  	- "10 [username]"
	 *  - 11: online user list response
	 *  	- "11 [user1, user2, ...]"
	 *  - 12: broadcast data
	 *  	- "12 [from] [data]"
	 *  - 13: broadcast response data
	 *  	- "13 OK NULL"
	 *  	- "13 BLOCKED [user1, user2,...]"
	 *  - 14: get ip address request
	 *  	- "14 from target"
	 *  - 15: reply to the get address request (from client to server)
	 *  	- "15 [replier] [sender] agree"
	 *  	- "15 [replier] [sender] deny"
	 *  - 16: reply to the sender (server to client receiver)
	 *  	- "16 replier agree ip port"
	 *  	- "16 replier deny NULL NULL"
	 *  - 17: private message
	 *  	- "17 [sender] [message]"
	 */
	public static String [] parse(String msgdata){
		msgdata = msgdata.trim();
		String [] ret = null;
		String type = msgdata.substring(0, 2);
		if (type.equals("99")){
			ret = new String [1];
			ret[0] = type;
		} else if (type.equals("02")||type.equals("00")||type.equals("01")||type.equals("03")
				||type.equals("04")||type.equals("06")||type.equals("07")||type.equals("09")
				||type.equals("10")||type.equals("11")){
			if (msgdata.length() < 4)
				return null;
			ret = new String[2];
			ret[0] = type;
			ret[1] = msgdata.substring(3);
		} else if (type.equals("05")||type.equals("08")||type.equals("15")) {
			ret = new String[4];
			ret[0] = type;
			String [] words = msgdata.split(" ");
			ret[1] = words[1];
			ret[2] = words[2];
			ret[3] = msgdata.substring(ret[0].length()+ret[1].length()+ret[2].length()+3);
		}
		else if (type.equals("12")|| type.equals("13")||type.equals("14")||type.equals("17")){
			String field1 = msgdata.split(" ")[1];
			ret = new String[3];
			ret[0] = type;
			ret[1] = field1;
			ret[2] = msgdata.substring(3+field1.length()+1);
		} else if (type.equals("16")){
			ret = msgdata.split(" ");
		}
		return ret;
	}
}
