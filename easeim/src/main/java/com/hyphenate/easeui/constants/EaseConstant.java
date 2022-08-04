/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hyphenate.easeui.constants;

public interface EaseConstant {
    String MESSAGE_ATTR_IS_VOICE_CALL = "is_voice_call";
    String MESSAGE_ATTR_IS_VIDEO_CALL = "is_video_call";


    String MESSAGE_ATTR_IS_BIG_EXPRESSION = "em_is_big_expression";
    String MESSAGE_ATTR_EXPRESSION_ID = "em_expression_id";

    String MESSAGE_ATTR_AT_MSG = "em_at_list";
    String MESSAGE_ATTR_VALUE_AT_MSG_ALL = "ALL";

    String FORWARD_MSG_ID = "forward_msg_id";
    String HISTORY_MSG_ID = "history_msg_id";

    int CHATTYPE_SINGLE = 1;
    int CHATTYPE_GROUP = 2;
    int CHATTYPE_CHATROOM = 3;

    String EXTRA_CHAT_TYPE = "chatType";
    String EXTRA_CONVERSATION_ID = "conversationId";
    String EXTRA_IS_ROAM = "isRoaming";

    String MESSAGE_TYPE_TXT = "txt";
    String MESSAGE_TYPE_EXPRESSION = "expression";
    String MESSAGE_TYPE_IMAGE = "image";
    String MESSAGE_TYPE_VIDEO = "video";
    String MESSAGE_TYPE_LOCATION = "location";
    String MESSAGE_TYPE_VOICE = "voice";
    String MESSAGE_TYPE_FILE = "file";
    String MESSAGE_TYPE_CMD = "cmd";
    String MESSAGE_TYPE_RECALL = "message_recall";
    String MESSAGE_TYPE_RECALLER = "message_recaller";
    String MESSAGE_TYPE_VOICE_CALL = "voice_call";
    String MESSAGE_TYPE_VIDEO_CALL = "video_call";
    String MESSAGE_TYPE_CONFERENCE_INVITE = "conference_invite";
    String MESSAGE_TYPE_LIVE_INVITE = "live_invite";

    String MESSAGE_FORWARD = "message_forward";

    String MESSAGE_CHANGE_RECEIVE = "message_receive";
    String MESSAGE_CHANGE_CMD_RECEIVE = "message_cmd_receive";
    String MESSAGE_CHANGE_SEND_SUCCESS = "message_success";
    String MESSAGE_CHANGE_SEND_ERROR = "message_error";
    String MESSAGE_CHANGE_SEND_PROGRESS = "message_progress";
    String MESSAGE_CHANGE_RECALL = "message_recall";
    String MESSAGE_CHANGE_CHANGE = "message_change";
    String MESSAGE_CHANGE_DELETE = "message_delete";
    String MESSAGE_CALL_SAVE = "message_call_save";
    String CONVERSATION_DELETE = "conversation_delete";
    String CONVERSATION_READ = "conversation_read";

    String GROUP_LEAVE = "group_leave";

    String DEFAULT_SYSTEM_MESSAGE_ID = "em_system";
    String DEFAULT_SYSTEM_MESSAGE_TYPE = "em_system_type";

    String ACCOUNT_CHANGE = "account_change";
    String ACCOUNT_REMOVED = "account_removed";
    String ACCOUNT_CONFLICT = "conflict";
    String ACCOUNT_FORBIDDEN = "user_forbidden";
    String ACCOUNT_KICKED_BY_CHANGE_PASSWORD = "kicked_by_change_password";
    String ACCOUNT_KICKED_BY_OTHER_DEVICE = "kicked_by_another_device";

    String EXTRA_CONFERENCE_ID = "confId";
    String EXTRA_CONFERENCE_PASS = "password";
    String EXTRA_CONFERENCE_INVITER = "inviter";
    String EXTRA_CONFERENCE_IS_CREATOR = "is_creator";
    String EXTRA_CONFERENCE_GROUP_ID = "group_id";
    String EXTRA_CONFERENCE_GROUP_EXIST_MEMBERS = "exist_members";

    String OP_INVITE = "invite";
    String OP_REQUEST_TOBE_SPEAKER = "request_tobe_speaker";
    String OP_REQUEST_TOBE_AUDIENCE = "request_tobe_audience";

    String EM_CONFERENCE_OP = "em_conference_op";
    String EM_CONFERENCE_ID = "em_conference_id";
    String EM_CONFERENCE_PASSWORD = "em_conference_password";
    String EM_CONFERENCE_TYPE = "em_conference_type";
    String EM_MEMBER_NAME = "em_member_name";
    String EM_NOTIFICATION_TYPE = "em_notification_type";

    String MSG_ATTR_CONF_ID = "conferenceId";
    String MSG_ATTR_CONF_PASS = EXTRA_CONFERENCE_PASS;
    String MSG_ATTR_EXTENSION = "msg_extension";

    String NEW_FRIENDS_USERNAME = "item_new_friends";
    String GROUP_USERNAME = "item_groups";
    String CHAT_ROOM = "item_chatroom";
    String CHAT_ROBOT = "item_robots";

    String NOTIFY_GROUP_INVITE_RECEIVE = "invite_receive";
    String NOTIFY_GROUP_INVITE_ACCEPTED = "invite_accepted";
    String NOTIFY_GROUP_INVITE_DECLINED = "invite_declined";
    String NOTIFY_GROUP_JOIN_RECEIVE = "invite_join_receive";
    String NOTIFY_CHANGE = "notify_change";

    String MESSAGE_GROUP_JOIN_ACCEPTED = "message_join_accepted";
    String MESSAGE_GROUP_AUTO_ACCEPT = "message_auto_accept";

    String CONTACT_REMOVE = "contact_remove";
    String CONTACT_ACCEPT = "contact_accept";
    String CONTACT_DECLINE = "contact_decline";
    String CONTACT_BAN = "contact_ban";
    String CONTACT_ALLOW = "contact_allow";

    String CONTACT_CHANGE = "contact_change";
    String CONTACT_ADD = "contact_add";
    String CONTACT_DELETE = "contact_delete";
    String CONTACT_UPDATE = "contact_update";
    String NICK_NAME_CHANGE = "nick_name_change";
    String AVATAR_CHANGE = "avatar_change";
    String REMOVE_BLACK = "remove_black";

    String GROUP_CHANGE = "group_change";
    String GROUP_OWNER_TRANSFER = "group_owner_transfer";
    String GROUP_SHARE_FILE_CHANGE = "group_share_file_change";

    String CHAT_ROOM_CHANGE = "chat_room_change";
    String CHAT_ROOM_DESTROY = "chat_room_destroy";

    String REFRESH_NICKNAME = "refresh_nickname";

    String MESSAGE_NOT_SEND = "message_not_send";

    String SYSTEM_MESSAGE_FROM = "from";
    String SYSTEM_MESSAGE_REASON = "reason";
    String SYSTEM_MESSAGE_STATUS = "status";
    String SYSTEM_MESSAGE_GROUP_ID = "groupId";
    String SYSTEM_MESSAGE_NAME = "name";
    String SYSTEM_MESSAGE_INVITER = "inviter";

    String USER_CARD_EVENT = "userCard";
    String USER_CARD_ID = "uid";
    String USER_CARD_NICK = "nickname";
    String USER_CARD_AVATAR = "avatar";

    String UNREAD_TOTAL = "unread_total";
    String UNREAD_EXCLUSIVE_GROUP = "unread_exclusive_group";
    String UNREAD_MY_CHAT = "unread_my_chat";

    String EXTRA_CONVERSATIONS_TYPE = "conversations_type";
    int CON_TYPE_ADMIN = 2;
    int CON_TYPE_MY_CHAT = 1;
    int CON_TYPE_EXCLUSIVE = 0;

    String IS_EXCLUSIVE = "isExclusive";

    String MESSAGE_ATTR_CALL_STATE = "callState";
    String MESSAGE_ATTR_CALL_USER = "callUser";

    String CONFERENCE_STATE_CREATE = "createCall";
    String CONFERENCE_STATE_END = "endCall";

    String CREATE_GROUP_PROMPT = "groupCreate";
    String CREATE_GROUP_NAME = "groupName";

    String MESSAGE_ATTR_USER_INFO = "userInfo";
    String MESSAGE_ATTR_USER_NAME = "im_username";
    String MESSAGE_ATTR_USER_NICK = "nick";
    String MESSAGE_ATTR_USER_AVATAR = "avatar";

    String NEW_GROUP_APPLY = "newGroupApply";

    String MESSAGE_ATTR_EVENT_TYPE = "eventType";

    String MESSAGE_ATTR_NO_PUSH = "noPush";
    String MESSAGE_ATTR_NO_PUSH_ID = "id";
    String EVENT_TYPE_GROUP_NO_PUSH = "groupNoPush";
    String EVENT_TYPE_USER_NO_PUSH = "userNoPush";

    String EVENT_TYPE_GROUP_INTRO = "groupIntro";
    String MESSAGE_ATTR_GROUP_ID = "groupId";
    String MESSAGE_ATTR_GROUP_INTRO = "introduction";
}
