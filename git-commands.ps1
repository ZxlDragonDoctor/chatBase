# Git Commit Script for ChatBase Private Chat Feature
# Run this script when git is available

Write-Host "=== Committing all changes with proper messages ===" -ForegroundColor Green

# Check if git is available
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "Git not found! Please install git first." -ForegroundColor Red
    exit 1
}

# Set working directory
$projectRoot = "D:\idea_java_project\chatBase"
Set-Location $projectRoot

# Initialize git if not already
if (-not (Test-Path ".git")) {
    git init
    git add -A
    git commit -m "chore: initial project snapshot"
}

# Commit 1: Database schema
git add sql/init-schema.sql
git commit -m "feat(db): add im_conversation table and extend group_message for private chat

- Create im_conversation table for tracking single-chat conversations
- Add conversation_type (group/single) and conversation_id columns to group_message
- Add index for conversation-based queries"

# Commit 2: New entities and services
git add src/main/java/com/zxl/chatbase/im/entity/ImConversation.java
git add src/main/java/com/zxl/chatbase/im/mapper/ImConversationMapper.java
git add src/main/java/com/zxl/chatbase/im/service/ImConversationService.java
git add src/main/java/com/zxl/chatbase/im/service/impl/ImConversationServiceImpl.java
git add src/main/java/com/zxl/chatbase/im/dto/ConversationSummaryVO.java
git commit -m "feat(im): add ImConversation entity, mapper, service with data isolation

- ImConversation entity mapped to im_conversation table
- Mapper with selectAccessibleConversations (data isolation via created_by)
- Service: getOrCreateConversation, updateLastMessage, listAccessibleConversations
- Data isolation: visible if created_by=currentUser OR created_by IS NULL"

# Commit 3: Update GroupMessage entity
git add src/main/java/com/zxl/chatbase/im/entity/GroupMessage.java
git add src/main/java/com/zxl/chatbase/im/mapper/GroupMessageMapper.java
git add src/main/java/com/zxl/chatbase/im/dto/GroupMessageItemVO.java
git commit -m "feat(im): extend GroupMessage with conversation_type and conversation_id

- Add conversationType and conversationId fields
- Add mapper methods: countByConversation, countAllPrivateMessages, selectDistinctPrivateConversations
- Update GroupMessageItemVO DTO"

# Commit 4: savePrivateMessage method
git add src/main/java/com/zxl/chatbase/im/service/GroupMessageSyncService.java
git add src/main/java/com/zxl/chatbase/im/service/impl/GroupMessageSyncServiceImpl.java
git commit -m "feat(im): add savePrivateMessage for single-chat message storage

- New savePrivateMessage sets conversation_type='single' and conversation_id
- Existing saveGroupMessage now explicitly sets conversation_type='group'"

# Commit 5: QQ Bot private message
git add src/main/java/com/zxl/chatbase/qq/QqBotWebSocketHandler.java
git commit -m "feat(qq): add private message support to QQ bot handler

- Remove group-only restriction, handle both group and private messages
- handleGroupMessage for group, handlePrivateMessage for private chat
- sendPrivateMessage via OneBot HTTP API
- Track conversations in im_conversation table"

# Commit 6: WeChat Work single chat
git add src/main/java/com/zxl/chatbase/wxroboot/webhook/service/impl/IntelligentRobotServiceImpl.java
git commit -m "feat(wecom): add single-chat support to enterprise WeChat robot

- Remove isFromGroup() restriction blocking single chat
- handleWeComGroupMessage for group, handleWeComSingleMessage for single"
- Use savePrivateMessage and ImConversationService"

# Commit 7: WeChat ilink refactor
git add src/main/java/com/zxl/chatbase/wx/service/WxIlinkService.java
git commit -m "refactor(wx): use proper conversation model for private chat

- Replace fake 'dm_' group ID with real conversation_id
- Use savePrivateMessage and ImConversationService for private chats"

# Commit 8: Console updates
git add src/main/java/com/zxl/chatbase/im/service/ImConsoleService.java
git add src/main/java/com/zxl/chatbase/im/service/impl/ImConsoleServiceImpl.java
git add src/main/java/com/zxl/chatbase/im/dto/ConsoleOverviewVO.java
git commit -m "feat(console): add private conversation management with data isolation

- listConversations and pagePrivateMessages methods
- ConsoleOverviewVO includes private message stats
- Group message queries filter by conversation_type='group'"

# Commit 9: REST endpoints
git add src/main/java/com/zxl/chatbase/controller/ImConsoleController.java
git commit -m "feat(api): add endpoints for private conversation management

- GET /api/console/conversations - list private conversations
- GET /api/console/conversations/messages - page private messages"

Write-Host "=== All commits created ===" -ForegroundColor Green
Write-Host ""
Write-Host "To push to remote:"
Write-Host "  git remote add origin <YOUR_REMOTE_URL>" -ForegroundColor Yellow
Write-Host "  git push -u origin master" -ForegroundColor Yellow
