<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">用户管理</div>
          <div class="anime-card-desc">查看和管理所有系统用户</div>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn ghost" @click="loadList">
            <RefreshCw :size="18" />
            <span>刷新</span>
          </button>
        </div>
      </div>

      <div v-if="err" class="anime-error" style="margin: 16px 28px;">{{ err }}</div>

      <div class="anime-card-body">
        <div class="filter-bar">
          <input v-model="keyword" class="anime-input" placeholder="搜索用户名/昵称/邮箱..." style="max-width: 300px;" @keyup.enter="loadList" />
          <button class="anime-btn ghost" @click="loadList">搜索</button>
        </div>

        <div v-if="loading" class="anime-empty">
          <span class="anime-loader-spinner"></span>
          <span class="anime-empty-text">加载中...</span>
        </div>

        <table v-else-if="list.length > 0" class="anime-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>状态</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in list" :key="user.id">
              <td class="anime-code">{{ user.id }}</td>
              <td>{{ user.username }}</td>
              <td>{{ user.nickname || '-' }}</td>
              <td>{{ user.email || '-' }}</td>
              <td>
                <select v-model="user.role" class="role-select" :disabled="editingId === user.id && roleSaving" @change="changeRole(user)">
                  <option value="user">用户</option>
                  <option value="admin">管理员</option>
                </select>
              </td>
              <td>
                <span class="anime-badge" :class="user.status ? 'green' : 'pink'" style="cursor: pointer;" @click="toggleStatus(user)">
                  {{ user.status ? '启用' : '禁用' }}
                </span>
              </td>
              <td>{{ formatTime(user.createTime) }}</td>
              <td>
                <button class="anime-btn ghost sm danger" @click="deleteUser(user)">
                  <Trash2 :size="14" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-else-if="!loading" class="anime-empty">
          <div class="anime-empty-icon">👥</div>
          <div class="anime-empty-text">暂无用户</div>
        </div>

        <div class="pagination" v-if="total > pageSize">
          <button class="anime-btn ghost sm" :disabled="pageNum <= 1" @click="pageNum--; loadList()">上一页</button>
          <span class="anime-code">第 {{ pageNum }} / {{ totalPages }} 页 · 共 {{ total }} 条</span>
          <button class="anime-btn ghost sm" :disabled="pageNum >= totalPages" @click="pageNum++; loadList()">下一页</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RefreshCw, Trash2 } from 'lucide-vue-next'
import { api } from '../api/client'

interface UserItem {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  role: string
  status: boolean
  createTime: string
}

const list = ref<UserItem[]>([])
const loading = ref(false)
const err = ref<string | null>(null)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const editingId = ref<number | null>(null)
const roleSaving = ref(false)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

function formatTime(time: string | null) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

async function loadList() {
  loading.value = true
  err.value = null
  try {
    const res = await api.get('/user/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: keyword.value || undefined
      }
    })
    const data = res.data
    if (data.success) {
      list.value = data.data.records || []
      total.value = data.data.total || 0
    } else {
      err.value = data.message || '加载失败'
    }
  } catch (e: any) {
    err.value = e?.response?.data?.message || '加载失败'
    list.value = []
  } finally {
    loading.value = false
  }
}

async function changeRole(user: UserItem) {
  editingId.value = user.id
  roleSaving.value = true
  try {
    await api.put(`/user/${user.id}/role`, null, { params: { role: user.role } })
  } catch { loadList() }
  finally {
    editingId.value = null
    roleSaving.value = false
  }
}

async function toggleStatus(user: UserItem) {
  const newStatus = !user.status
  const action = newStatus ? '启用' : '禁用'
  if (!confirm(`确定${action}用户 "${user.username}"？`)) return
  try {
    await api.put(`/user/${user.id}/status`, null, { params: { status: newStatus } })
    user.status = newStatus
  } catch (e: any) {
    err.value = e?.response?.data?.message || '操作失败'
  }
}

async function deleteUser(user: UserItem) {
  if (!confirm(`确定删除用户 "${user.username}"？此操作不可恢复。`)) return
  try {
    await api.delete(`/user/${user.id}/remove`)
    loadList()
  } catch (e: any) {
    err.value = e?.response?.data?.message || '删除失败'
  }
}

onMounted(loadList)
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.role-select {
  background: var(--anime-bg);
  box-shadow: var(--anime-shadow-sm);
  border: none;
  border-radius: var(--anime-radius-lg);
  padding: 4px 8px;
  font-size: 13px;
  color: var(--anime-text-primary);
  outline: none;
  cursor: pointer;
}
.role-select:focus {
  box-shadow: var(--anime-shadow-pink);
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}
.anime-btn.sm {
  padding: 4px 10px;
  font-size: 13px;
}
</style>
