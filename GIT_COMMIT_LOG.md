# Git Commit History

Run `git-commands.ps1` in PowerShell to execute all commits and push to remote.

## Commit 1: Database schema - add private chat support
```
git add sql/init-schema.sql
git commit -m "feat(db): add im_conversation table and extend group_message for private chat

- Create im_conversation table for tracking single-chat conversations
- Add conversation_type (group/single) and conversation_id columns to group_message
- Add index for conversation-based queries
- Maintain backward compatibility with existing group messages"
```

## Commit 2: New entities and services for private chat
```
git add src/main/java/com/zxl/chatbase/im/entity/ImConversation.java
git add src/main/java/com/zxl/chatbase/im/mapper/ImConversationMapper.java
git add src/main/java/com/zxl/chatbase/im/service/ImConversationService.java
git add src/main/java/com/zxl/chatbase/im/service/impl/ImConversationServiceImpl.java
git add src/main/java/com/zxl/chatbase/im/dto/ConversationSummaryVO.java
git commit -m "feat(im): add ImConversation entity, mapper, service with data isolation

- New ImConversation entity mapped to im_conversation table
- Mapper with selectAccessibleConversations for data isolation (created_by filter)
- Service with getOrCreateConversation, updateLastMessage, listAccessibleConversations
- DTO ConversationSummaryVO for frontend display
- Data isolation follows existing pattern: visible if created_by=currentUser OR created_by IS NULL"
```

## Commit 3: Update GroupMessage entity for conversation type
```
git add src/main/java/com/zxl/chatbase/im/entity/GroupMessage.java
git add src/main/java/com/zxl/chatbase/im/mapper/GroupMessageMapper.java
git add src/main/java/com/zxl/chatbase/im/dto/GroupMessageItemVO.java
git commit -m "feat(im): extend GroupMessage entity with conversation_type and conversation_id

- Add conversationType and conversationId fields to GroupMessage entity
- Add countByConversation, countAllPrivateMessages, selectDistinctPrivateConversations to mapper
- Update GroupMessageItemVO DTO with new fields
- Update mapper queries to filter group vs private messages"
```

## Commit 4: Add savePrivateMessage method to sync service
```
git add src/main/java/com/zxl/chatbase/im/service/GroupMessageSyncService.java
git add src/main/java/com/zxl/chatbase/im/service/impl/GroupMessageSyncServiceImpl.java
git commit -m "feat(im): add savePrivateMessage method for single-chat message storage

- New interface method savePrivateMessage for storing private chat messages
- Implementation sets conversation_type='single' and conversation_id
- Existing saveGroupMessage now explicitly sets conversation_type='group'
- Maintains backward compatibility with existing message storage"
```

## Commit 5: Add QQ Bot private message support
```
git add src/main/java/com/zxl/chatbase/qq/QqBotWebSocketHandler.java
git commit -m "feat(qq): add private message support to QQ bot handler

- Remove group-only restriction in handleTextMessage
- Add handleGroupMessage method for existing group chat logic
- Add handlePrivateMessage method for new private chat support
- Add sendPrivateMessage via OneBot HTTP API (message_type=private)
- Add getDefaultAppId for private chat (no group binding)
- Track private conversations in im_conversation table"
```

## Commit 6: Add WeChat Work single-chat support
```
git add src/main/java/com/zxl/chatbase/wxroboot/webhook/service/impl/IntelligentRobotServiceImpl.java
git commit -m "feat(wecom): add single-chat support to enterprise WeChat robot

- Remove isFromGroup() restriction that blocked single chat messages
- Add handleWeComGroupMessage for existing group logic
- Add handleWeComSingleMessage for new single chat support
- Use savePrivateMessage + ImConversationService for private conversations
- Add getDefaultAppId helper method"
```

## Commit 7: Refactor WeChat ilink private chat handling
```
git add src/main/java/com/zxl/chatbase/wx/service/WxIlinkService.java
git commit -m "refactor(wx): use proper conversation tracking for private chat

- Replace fake 'dm_' group ID prefix with proper conversation_id
- Use savePrivateMessage for private chat messages
- Track conversations in im_conversation table via ImConversationService
- Clean up groupId logic for private vs group messages"
```

## Commit 8: Update IM Console with private chat management
```
git add src/main/java/com/zxl/chatbase/im/service/ImConsoleService.java
git add src/main/java/com/zxl/chatbase/im/service/impl/ImConsoleServiceImpl.java
git add src/main/java/com/zxl/chatbase/im/dto/ConsoleOverviewVO.java
git commit -m "feat(console): add private conversation listing and data isolation

- Add listConversations and pagePrivateMessages to ImConsoleService
- ConsoleOverviewVO now includes totalPrivateMessages and distinctPrivateConversations
- ImConsoleServiceImpl updated with ImConversationMapper for private chat queries
- Filter group message stats by conversation_type='group' only"
```

## Commit 9: Add console REST endpoints for private conversations
```
git add src/main/java/com/zxl/chatbase/controller/ImConsoleController.java
git commit -m "feat(api): add REST endpoints for private chat management

- GET /api/console/conversations - list accessible private conversations
- GET /api/console/conversations/messages - page private messages by conversationId"
```

## Push to remote (after all commits)
```
git remote add origin <YOUR_REMOTE_URL>
git push -u origin master
```
