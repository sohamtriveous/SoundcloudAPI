package com.soundcloud.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Http;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;

public class SoundcloudUtils<Like> {

	private enum Type {
		USER, TRACK, PLAYLIST, LIKE, COMMENT, GROUP
	}

	private enum Rest {
		GET, PUT, POST, DELETE
	}

	public ApiWrapper wrapper;
	protected JsonParser parser;
	protected String app_client_id;
	protected String app_client_secret;
	Context context;
	Gson gson = new Gson();
	Integer trackid = 104348680;

	public SoundcloudUtils(Context context) {
		this.context = context;
	}
	
	public void auth(String clientId, String clientSecret, String redirectUri, String access, String refresh) {
		Token token = new Token(access, refresh);
		wrapper = new ApiWrapper(clientId, clientSecret, URI.create(redirectUri), token);
	}

	public ApiWrapper login(Context context, String username, String password) {
		app_client_id = SoundcloudConstants.CLIENT_ID;

		wrapper = new ApiWrapper(SoundcloudConstants.CLIENT_ID, SoundcloudConstants.CLIENT_SECRET, null, null);
		Token token;
		try {
			token = wrapper.login(username, password);
			ApiWrapper newWrapper = new ApiWrapper(
					SoundcloudConstants.CLIENT_ID,
					SoundcloudConstants.CLIENT_SECRET, null, token);
			return newWrapper;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			System.out.println(exceptionAsString);
		}
		return null;
	}

	public static boolean uploadFile(ApiWrapper wrapper, String filePath) {
		try {
			final File file = new File(filePath);
			if (!file.exists())
				throw new IOException("The file `" + file + "` does not exist");
			else {
				Log.d("MainActivity", "Uploading file...");
			}

			String title = file.getName();

			HttpResponse resp = wrapper.post(Request
					.to(Endpoints.TRACKS)
					.add(Params.Track.TITLE,
							"track "+title.substring(0, title.lastIndexOf(".")))
					.add(Params.Track.TAG_LIST, "demo upload")
					.withFile(Params.Track.ASSET_DATA, file));

			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
				System.out.println("\n201 Created "
						+ resp.getFirstHeader("Location").getValue());

				// dump the representation of the new track
				System.out.println("\n" + Http.getJSON(resp).toString(4));
				return true;
			} else {
				System.err.println("Invalid status received: "
						+ resp.getStatusLine());
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * The core of the library.
	 * 
	 * @param api
	 * @param rest
	 * @param value
	 * @param filters
	 * @return
	 */
	private <T> T api(String api, Rest rest, Object value, String[] filters) {
		if (api.length() > 0) {
			if (api.startsWith("/") == false) {
				api = "/" + api;
			}
			if (api.charAt(api.length() - 1) == '/') {
				api = api.substring(0, api.length() - 1);
			}
			api = api.replace(".format", ".json").replace(".xml", ".json");
			if (api.indexOf(".json") == -1) {
				api += ".json";
			}
		} else {
			return null;
		}

		Type type = null;
		if (api.matches("^/me.json")
				|| api.matches("^/me/(followings(/[0-9]+)?|followers(/[0-9]+)?).json")
				|| api.matches("^/users(/[0-9]+)?.json")
				|| api.matches("^/users/([0-9]+)/(followings|followers).json")
				|| api.matches("^/groups/([0-9]+)/(users|moderators|members|contributors).json")) {
			type = Type.USER;
		} else if (api.matches("^/tracks(/[0-9]+)?.json")
				|| api.matches("^/me/(tracks|favorites)(/[0-9]+)?.json")
				|| api.matches("^/users/([0-9]+)/(tracks|favorites).json")) {
			type = Type.TRACK;
		} else if (api.matches("^/playlists(/[0-9]+)?.json")
				|| api.matches("^/me/playlists.json")
				|| api.matches("^/users/([0-9]+)/playlists.json")
				|| api.matches("^/groups/([0-9]+)/tracks.json")) {
			type = Type.PLAYLIST;
		} else if (api.matches("^/likes/([0-9]+).json")
				|| api.matches("^/me/likes.json")
				|| api.matches("^/tracks/([0-9]+)/likes.json")) {
			type = Type.LIKE;
		} else if (api.matches("^/comments/([0-9]+).json")
				|| api.matches("^/me/comments.json")
				|| api.matches("^/tracks/([0-9]+)/comments.json")) {
			type = Type.COMMENT;
		} else if (api.matches("^/groups(/[0-9]+)?.json")
				|| api.matches("^/me/groups.json")
				|| api.matches("^/users/([0-9]+)/groups.json")) {
			type = Type.GROUP;
		}
		if (type == null) {
			return null;
		}

		if (filters != null) {
			if (filters.length > 0 && filters.length % 2 == 0) {
				api += "?";
				for (int i = 0, l = filters.length; i < l; i += 2) {
					if (i != 0) {
						api += "&";
					}
					api += (filters[i] + "=" + filters[i + 1]);
				}
			}
		}

		try {

			Request resource;
			HttpResponse response;
			String klass, content;

			switch (rest) {
			case GET:
				response = wrapper.get(Request.to(api));

				if (response.getStatusLine().getStatusCode() == 303) { // recursive better
					api = (String) (response.getFirstHeader("Location")
							.getValue() + ".json").replace(
							"https://api.soundcloud.com", "");
					response = wrapper.get(Request.to(api));
				}

				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String json = (String) (Http.formatJSON(Http
                            .getString(response))).trim();

					if (json.startsWith("[") && json.endsWith("]")) {
						// JSONObject object = new JSONObject(json);
						JSONArray data = new JSONArray(json);

						if (data.length() > 0) {
							switch (type) {
							case USER:
								ArrayList<User> users = new ArrayList<User>();
								for (int i = 0, l = data.length(); i < l; i++) {
									String tuple = data.get(i).toString();
									User user = gson
											.fromJson(tuple, User.class);
									users.add(user);
								}
								return (T) users;
							case TRACK:
								ArrayList<Track> tracks = new ArrayList<Track>();
								for (int i = 0, l = data.length(); i < l; i++) {
									String tuple = data.get(i).toString();
									Track track = gson.fromJson(tuple,
											Track.class);
									track.setSoundCloud(this);
									tracks.add(track);
								}
								return (T) tracks;
							case PLAYLIST:
								ArrayList<Playlist> playlists = new ArrayList<Playlist>();
								for (int i = 0, l = data.length(); i < l; i++) {
									String tuple = data.get(i).toString();
									Playlist playlist = gson.fromJson(tuple,
											Playlist.class);
									playlists.add(playlist);
								}
								return (T) playlists;
							case LIKE:
								ArrayList<Like> likes = new ArrayList<Like>();
								for (int i = 0, l = data.length(); i < l; i++) {
									String tuple = data.get(i).toString();
									Likes like = gson.fromJson(tuple,
											Likes.class);
									likes.add((Like) like);
								}
								return (T) likes;

							case COMMENT:
								ArrayList<Comment> comments = new ArrayList<Comment>();
								for (int i = 0, l = data.length(); i < l; i++) {
									String tuple = data.get(i).toString();
									Comment comment = gson.fromJson(tuple,
											Comment.class);
									comments.add(comment);
								}
								return (T) comments;
							case GROUP:
								ArrayList<Group> groups = new ArrayList<Group>();
								for (int i = 0, l = data.length(); i < l; i++) {
									String tuple = data.get(i).toString();
									Group group = gson.fromJson(tuple,
											Group.class);
									groups.add(group);
								}
								return (T) groups;
							default:
								return null;
							}

						}

					} else {

						switch (type) {
						case USER:
							User user = gson.fromJson(json, User.class);
							return (T) user;
						case TRACK:
							Track track = gson.fromJson(json, Track.class);
							track.setSoundCloud(this);
							return (T) track;
						case PLAYLIST:
							Playlist playlist = gson.fromJson(json,
									Playlist.class);
							return (T) playlist;
						case LIKE:
							Likes like = gson.fromJson(json, Likes.class);
							return (T) like;
						case COMMENT:
							Comment comment = gson
									.fromJson(json, Comment.class);
							return (T) comment;
						case GROUP:
							Group group = gson.fromJson(json, Group.class);
							return (T) group;
						default:
							return null;
						}

					}
				} else {
					System.err.println("Invalid status received: "
							+ response.getStatusLine());
				}
				break;
			case POST:

				klass = value.getClass().getName();
				klass = klass.substring((klass.lastIndexOf('.') + 1));

				if (klass.equals("Track")) {
					Track track = ((Track) value);
					resource = Request
							.to(Endpoints.TRACKS)
							.add(Params.Track.TITLE, track.getTitle())
							.add(Params.Track.TAG_LIST, track.getTagList())
							.withFile(Params.Track.ASSET_DATA,
									new File(track.asset_data));
				} else if (klass.equals("Comment")) {
					content = gson.toJson(value);
					content = "{\"like\":" + content + "}";
					content = "{\"comment\":" + content + "}";
					resource = Request.to(api.replace(".json", ""))
							.withContent(content, "application/json");
				} else {
					return null;
				}

				response = wrapper.post(resource);

				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
					String json = (String) (Http.formatJSON(Http
                            .getString(response))).trim();
					switch (type) {
					case TRACK:
						Track track = gson.fromJson(json, Track.class);
						track.setSoundCloud(this);
						return (T) track;
					case COMMENT:
						Comment comment = gson.fromJson(json, Comment.class);
						return (T) comment;
					default:
						return null;
					}

				} else {
					System.err.println("Invalid status received: "
							+ response.getStatusLine());
				}

				break;
			case PUT:

				if (value != null) {

					klass = value.getClass().getName();
					klass = klass.substring((klass.lastIndexOf('.') + 1));

					content = gson.toJson(value);

					if (klass.equals("User")) {
						content = "{\"user\":" + content + "}";
					} else if (klass.equals("Track")) {
						content = "{\"track\":" + content + "}";
					} else {
						return null;
					}

					resource = Request.to(api.replace(".json", ""))
							.withContent(content, "application/json");
				} else {
					resource = Request.to(api.replace(".json", ""));
				}

				response = wrapper.put(resource);

				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String json = (String) (Http.formatJSON(Http
                            .getString(response))).trim();

					switch (type) {
					case USER:
						User user = gson.fromJson(json, User.class);
						return (T) user;
					case TRACK:
						Track track = gson.fromJson(json, Track.class);
						track.setSoundCloud(this);
						return (T) track;
					default:
						return null;
					}
				} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
					return (T) new Boolean(true);
				} else {
					System.err.println("Invalid status received: "
							+ response.getStatusLine());
				}

				break;
			case DELETE:
				response = wrapper.delete(Request.to(api));

				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					return (T) new Boolean(true);
				}
				return (T) new Boolean(false);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * API access to GET (REST) data.
	 * 
	 * @param api
	 * @return
	 */
	public <T> T get(String api) {
		return this.api(api, Rest.GET, null, null);
	}

	/**
	 * API access with filters to GET (REST) data.
	 * 
	 * @param api
	 * @param filters
	 * @return
	 */
	public <T> T get(String api, String[] filters) {
		return this.api(api, Rest.GET, null, filters);
	}

	/**
	 * API access to POST (REST) new data.
	 * 
	 * @param api
	 * @return
	 */
	public <T> T post(String api, Object value) {
		return this.api(api, Rest.POST, value, null);
	}

	/**
	 * API access to PUT (REST) new data.
	 * 
	 * @param api
	 * @return
	 */
	public <T> T put(String api) {
		return this.api(api, Rest.PUT, null, null);
	}

	/**
	 * API access to PUT (REST) new data.
	 * 
	 * @param api
	 * @return
	 */
	public <T> T put(String api, Object value) {
		return this.api(api, Rest.PUT, value, null);
	}

	/**
	 * API access to DELETE (REST) data.
	 * 
	 * @param api
	 * @return
	 */
	public Boolean delete(String api) {
		return (Boolean) this.api(api, Rest.DELETE, null, null);
	}

	/**
	 * Get data about you.
	 * 
	 * @return
	 */
	public User getMe() {
		return this.get("me");
	}

	/**
	 * Get your followings.
	 * 
	 * @param offset
	 * @param limit
	 * @return
	 */
	public ArrayList<User> getMeFollowings(Integer offset, Integer limit) {
		return this.get("me/followings",
				new String[] { "limit", Integer.toString(limit), "offset",
						Integer.toString(offset) });
	}

	/**
	 * Get your last 50 followings.
	 * 
	 * @return
	 */
	public ArrayList<User> getMeFollowings() {
		return this.getMeFollowings(0, 50);
	}

	/**
	 * Get a specific following.
	 * 
	 * @param contact_id
	 * @return
	 */
	public User getMeFollowing(Integer contact_id) {
		return this.get("me/followings/" + Integer.toString(contact_id));
	}

	/**
	 * Get your followers.
	 * 
	 * @param offset
	 * @param limit
	 * @return
	 */
	public ArrayList<User> getMeFollowers(Integer offset, Integer limit) {
		return this.get("me/followers",
				new String[] { "limit", Integer.toString(limit), "offset",
						Integer.toString(offset) });
	}

	/**
	 * Get your last 50 followers.
	 * 
	 * @return
	 */
	public ArrayList<User> getMeFollowers() {
		return this.getMeFollowers(0, 50);
	}

	/**
	 * Get a specific follower.
	 * 
	 * @param contact_id
	 * @return
	 */
	public User getMeFollower(Integer contact_id) {
		return this.get("me/followers/" + Integer.toString(contact_id));
	}

	/**
	 * Get your favorite tracks.
	 * 
	 * @param limit
	 * @param offset
	 * @return
	 */
	public ArrayList<Track> getMeFavorites(Integer offset, Integer limit) {
		return this.get(
				"me/favorites",
				new String[] { "order", "created_at", "limit",
						Integer.toString(limit), "offset",
						Integer.toString(offset) });
	}

	/**
	 * Get your last 50 favorite tracks.
	 * 
	 * @return
	 */
	public ArrayList<Track> getMeFavorites() {
		return this.getMeFavorites(0, 50);
	}

	/**
	 * Get your last tracks.
	 * 
	 * @param offset
	 * @param limit
	 * @return
	 */
	public ArrayList<Track> getMeTracks(Integer offset, Integer limit) {
		return this.get(
				"me/tracks",
				new String[] { "order", "created_at", "limit",
						Integer.toString(limit), "offset",
						Integer.toString(offset) });
	}

	/**
	 * Get your last 50 tracks.
	 * 
	 * @return
	 */
	public ArrayList<Track> getMeTracks() {
		return this.getMeTracks(0, 50);
	}

	/**
	 * Get your playlists
	 * 
	 * @return
	 */
	public ArrayList<Playlist> getMePlaylists(Integer offset, Integer limit) {
		return this.get(
				"me/playlists",
				new String[] { "order", "created_at", "limit",
						Integer.toString(limit), "offset",
						Integer.toString(offset) });
	}

	/**
	 * Get your last 50 playlists.
	 * 
	 * @return
	 */
	public ArrayList<Playlist> getMePlaylists() {
		return this.getMePlaylists(0, 50);
	}

	/**
	 * Get your groups.
	 * 
	 * @return
	 */
	public ArrayList<Group> getMeGroups(Integer offset, Integer limit) {
		return this.get(
				"me/groups",
				new String[] { "order", "created_at", "limit",
						Integer.toString(limit), "offset",
						Integer.toString(offset) });
	}

	/**
	 * Get your last 50 groups.
	 * 
	 * @return
	 */
	public ArrayList<Group> getMeGroups() {
		return this.getMeGroups(0, 50);
	}

	public ArrayList<Comment> getMeLikes(Integer offset, Integer limit) {
		return this.get(
				"me/Likes",
				new String[] { "order", "created_at", "limit",
						Integer.toString(limit), "offset",
						Integer.toString(offset) });
	}

	/**
	 * Get your last 50 likes.
	 * 
	 * @return
	 */
	public ArrayList<Comment> getMeLikes() {
		return this.getMeLikes(0, 50);
	}

	public ArrayList<Like> getLikesFromTrack(Integer track_id) {
		return this.get("me/" + Integer.toString(track_id) + "Likes");
	}

	/**
	 * Get your comments.
	 * 
	 * @return
	 */
	public ArrayList<Comment> getMeComments(Integer offset, Integer limit) {
		return this.get(
				"me/comments",
				new String[] { "order", "created_at", "limit",
						Integer.toString(limit), "offset",
						Integer.toString(offset) });
	}

	/**
	 * Get your last 50 comments.
	 * 
	 * @return
	 */
	public ArrayList<Comment> getMeComments() {
		return this.getMeComments(0, 50);
	}

	/**
	 * Get comments from specific track.
	 * 
	 * @param track_id
	 * @return
	 */
	public ArrayList<Comment> getCommentsFromTrack(Integer track_id) {
		return this.get("me/" + Integer.toString(track_id) + "comments");
	}

	/**
	 * Get a specific playlist.
	 * 
	 * @param playlist_id
	 * @return
	 */
	public Playlist getPlaylist(Integer playlist_id) {
		return this.get("playlists/" + Integer.toString(playlist_id));
	}

	/**
	 * Get last playlists.
	 * 
	 * @param offset
	 * @param limit
	 * @return
	 */
	public ArrayList<Playlist> getPlaylists(Integer offset, Integer limit) {
		return this.get(
				"playlists",
				new String[] { "order", "created_at", "limit",
						Integer.toString(limit), "offset",
						Integer.toString(offset) });
	}

	/**
	 * Get last 50 playlists.
	 * 
	 * @return
	 */
	public ArrayList<Playlist> getPlaylists() {
		return this.getPlaylists(0, 50);
	}

	/**
	 * Get a specific user.
	 *
	 * @param contact_id
	 * @return
	 */
	public User getUser(Integer contact_id) {
		return this.get("users/" + Integer.toString(contact_id));
	}

	/**
	 * Get last users.
	 * 
	 * @param limit
	 * @param offset
	 * @return
	 */
	public ArrayList<User> getUsers(Integer offset, Integer limit) {
		return this.get("users", new String[] { "order", "created_at", "limit",
				Integer.toString(limit), "offset", Integer.toString(offset) });
	}

	/**
	 * Get last users.
	 * 
	 * @return
	 */
	public ArrayList<User> getUsers() {
		return this.getUsers(0, 50);
	}

	/**
	 * Simple user search.
	 * 
	 * @param username
	 * @return
	 */
	public ArrayList<User> findUser(String username) {
		return this.get("users", new String[] { "q", username });
	}

	/**
	 * Get a specific track.
	 * 
	 * @param track_id
	 * @return
	 */
	public Track getTrack(Integer track_id) {
		return this.get("tracks/" + Integer.toString(track_id));
	}

	/**
	 * Get last tracks.
	 * 
	 * @param offset
	 * @param limit
	 * @return
	 */
	public ArrayList<Track> getTracks(Integer offset, Integer limit) {
		return this.get(
				"tracks",
				new String[] { "order", "created_at", "limit",
						Integer.toString(limit), "offset",
						Integer.toString(offset) });
	}

	/**
	 * Get last 50 tracks.
	 * 
	 * @return
	 */
	public ArrayList<Track> getTracks() {
		return this.getTracks(0, 50);
	}

	/**
	 * Get tracks from specific group.
	 * 
	 * @param group_id
	 * @return
	 */
	public ArrayList<Track> getTracksFromGroup(Integer group_id) {
		return this.get("groups/" + Integer.toString(group_id) + "/tracks");
	}

	/**
	 * Simple track search.
	 * 
	 * @param title
	 * @return
	 */
	public ArrayList<Track> findTrack(String title) {
		return this.get("tracks", new String[] { "q", title });
	}

	/**
	 * Get a specific group.
	 * 
	 * @param id
	 * @return
	 */
	public Group getGroup(Integer id) {
		return this.get("groups/" + Integer.toString(id));
	}

	/**
	 * Get last groups.
	 * 
	 * @param offset
	 * @param limit
	 * @return
	 */
	public ArrayList<Group> getGroups(Integer offset, Integer limit) {
		return this.get(
				"groups",
				new String[] { "order", "created_at", "limit",
						Integer.toString(limit), "offset",
						Integer.toString(offset) });
	}

	/**
	 * Get last 50 groups.
	 * 
	 * @return
	 */
	public ArrayList<Group> getGroups() {
		return this.getGroups(0, 50);
	}

	/**
	 * Simple user search.
	 * 
	 * @param name
	 * @return
	 */
	public ArrayList<Group> findGroup(String name) {
		return this.get("groups", new String[] { "q", name });
	}

	/**
	 * Update your account.
	 * 
	 * @param user
	 * @return
	 */
	public User putMe(User user) {
		return this.put("me", user);
	}

	/**
	 * Favor a specific track.
	 * 
	 * @param track_id
	 * @return
	 */
	public Boolean putFavoriteTrack(Integer track_id) {
		return this.put("me/favorites/" + Integer.toString(track_id));
	}

	/**
	 * Post a new track.
	 * 
	 * @param track
	 * @return
	 */
	public Track postTrack(Track track) {
		return this.post("tracks", track);
	}

	/**
	 * Post a new comment to a specific track.
	 * 
	 * @param track_id
	 * @param comment
	 * @return
	 */
	public Comment postCommentToTrack(Integer track_id, Comment comment) {
		return this.post("tracks/" + Integer.toString(track_id) + "/comments",
				comment);
	}

	/**
	 * Delete a specific track.
	 * 
	 * @param track_id
	 * @return
	 */
	public Boolean deleteTrack(Integer track_id) {
		return this.delete("tracks/" + Integer.toString(track_id));
	}

	/**
	 * Remove a specific favorite track.
	 * 
	 * @param track_id
	 * @return
	 */
	public Boolean deleteFavoriteTrack(Integer track_id) {
		return this.delete("me/favorites/" + Integer.toString(track_id));
	}

}