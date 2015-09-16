/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.kurento.room.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.client.JsonRpcWSConnectionListener;
import org.kurento.room.client.internal.JsonRoomUtils;
import org.kurento.room.client.internal.Notification;
import org.kurento.room.internal.ProtocolElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Java client for the room server.
 * 
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class KurentoRoomClient {

	private static final Logger log = LoggerFactory
			.getLogger(KurentoRoomClient.class);


	private JsonRpcClient client;
	private ServerJsonRpcHandler handler;

	public KurentoRoomClient(String wsUri) {
		this(new JsonRpcClientWebSocket(wsUri,
				new JsonRpcWSConnectionListener() {

					@Override
					public void reconnected(boolean sameServer) {}

					@Override
					public void disconnected() {
						log.warn("JsonRpcWebsocket connection: Disconnected");
					}

					@Override
					public void connectionFailed() {
						log.warn("JsonRpcWebsocket connection: Connection failed");
					}

					@Override
					public void connected() {}
				}));
	}

	public KurentoRoomClient(JsonRpcClient client) {
		this.client = client;
		this.handler = new ServerJsonRpcHandler();
		this.client.setServerRequestHandler(this.handler);
	}

	public KurentoRoomClient(JsonRpcClient client, ServerJsonRpcHandler handler) {
		this.client = client;
		this.handler = handler;
		this.client.setServerRequestHandler(this.handler);
	}

	public void close() throws IOException {
		this.client.close();
	}

	public Map<String, List<String>> joinRoom(String roomName, String userName)
			throws IOException {
		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.JOINROOM_ROOM_PARAM, roomName);
		params.addProperty(ProtocolElements.JOINROOM_USER_PARAM, userName);
		JsonElement result =
				client.sendRequest(ProtocolElements.JOINROOM_METHOD, params);
		Map<String, List<String>> peers = new HashMap<String, List<String>>();
		JsonArray jsonPeers =
				JsonRoomUtils.getResponseProperty(result, "value",
						JsonArray.class);
		if (jsonPeers.size() > 0) {
			Iterator<JsonElement> peerIt = jsonPeers.iterator();
			while (peerIt.hasNext()) {
				JsonElement peer = peerIt.next();
				String peerId =
						JsonRoomUtils.getResponseProperty(peer,
								ProtocolElements.JOINROOM_PEERID_PARAM,
								String.class);
				List<String> streams = new ArrayList<String>();
				JsonArray jsonStreams =
						JsonRoomUtils.getResponseProperty(peer,
								ProtocolElements.JOINROOM_PEERSTREAMS_PARAM,
								JsonArray.class, true);
				if (jsonStreams != null) {
					Iterator<JsonElement> streamIt = jsonStreams.iterator();
					while (streamIt.hasNext())
						streams.add(JsonRoomUtils.getResponseProperty(
								streamIt.next(),
								ProtocolElements.JOINROOM_PEERSTREAMID_PARAM,
								String.class));
				}
				peers.put(peerId, streams);
			}
		}
		return peers;
	}

	public void leaveRoom() throws IOException {
		client.sendRequest(ProtocolElements.LEAVEROOM_METHOD, new JsonObject());
	}

	public String publishVideo(String sdpOffer, boolean doLoopback)
			throws IOException {
		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.PUBLISHVIDEO_SDPOFFER_PARAM,
				sdpOffer);
		params.addProperty(ProtocolElements.PUBLISHVIDEO_DOLOOPBACK_PARAM,
				doLoopback);
		JsonElement result =
				client.sendRequest(ProtocolElements.PUBLISHVIDEO_METHOD, params);
		return JsonRoomUtils.getResponseProperty(result,
				ProtocolElements.PUBLISHVIDEO_SDPANSWER_PARAM, String.class);
	}

	public void unpublishVideo() throws IOException {
		client.sendRequest(ProtocolElements.UNPUBLISHVIDEO_METHOD,
				new JsonObject());
	}

	// sender should look like 'username_streamId'
	public String receiveVideoFrom(String sender, String sdpOffer)
			throws IOException {
		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.RECEIVEVIDEO_SENDER_PARAM, sender);
		params.addProperty(ProtocolElements.RECEIVEVIDEO_SDPOFFER_PARAM,
				sdpOffer);
		JsonElement result =
				client.sendRequest(ProtocolElements.RECEIVEVIDEO_METHOD, params);
		return JsonRoomUtils.getResponseProperty(result,
				ProtocolElements.RECEIVEVIDEO_SDPANSWER_PARAM, String.class);
	}

	// sender should look like 'username_streamId'
	public void unsubscribeFromVideo(String sender) throws IOException {
		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.UNSUBSCRIBEFROMVIDEO_SENDER_PARAM,
				sender);
		client.sendRequest(ProtocolElements.UNSUBSCRIBEFROMVIDEO_METHOD, params);
	}

	public void onIceCandidate(String endpointName, String candidate,
			String sdpMid, int sdpMLineIndex) throws IOException {
		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.ONICECANDIDATE_EPNAME_PARAM,
				endpointName);
		params.addProperty(ProtocolElements.ONICECANDIDATE_CANDIDATE_PARAM,
				candidate);
		params.addProperty(ProtocolElements.ONICECANDIDATE_SDPMIDPARAM, sdpMid);
		params.addProperty(ProtocolElements.ONICECANDIDATE_SDPMLINEINDEX_PARAM,
				sdpMLineIndex);
		client.sendRequest(ProtocolElements.ONICECANDIDATE_METHOD, params);
	}

	public void sendMessage(String userName, String roomName, String message)
			throws IOException {
		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.SENDMESSAGE_USER_PARAM, userName);
		params.addProperty(ProtocolElements.SENDMESSAGE_ROOM_PARAM, roomName);
		params.addProperty(ProtocolElements.SENDMESSAGE_MESSAGE_PARAM, message);
		client.sendRequest(ProtocolElements.SENDMESSAGE_ROOM_METHOD, params);
	}

	public JsonElement customRequest(JsonObject customReqParams)
			throws IOException {
		return client.sendRequest(ProtocolElements.CUSTOMREQUEST_METHOD,
				customReqParams);
	}

	/**
	 * Polls the notifications list maintained by this client to obtain new
	 * events sent by server. This method blocks until there is a notification
	 * to return. This is a one-time operation for the returned element.
	 * 
	 * @return a server notification object, null when interrupted while waiting
	 */
	public Notification getServerNotification() {
		return this.handler.getNotification();
	}
}