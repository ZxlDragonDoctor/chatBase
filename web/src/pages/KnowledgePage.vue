<template>
  <div class="anime-page-shell">
    <section class="anime-card" :aria-busy="loading || syncing || uploadLoading || docLoading">
      <div class="anime-card-header">
        <div>
          <h1 class="anime-card-title">知识库管理</h1>
          <p class="anime-card-desc">分类管理 · 知识库管理 · 文档同步 · FAQ配置</p>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn blue" @click="doSyncFromDify" :disabled="syncing">
            <Download :size="18" />
            <span v-if="syncing">同步中...</span>
            <span v-else>从Dify同步</span>
          </button>
          <button v-if="activeTab === 'category'" class="anime-btn primary" @click="showCreateCategory = true">
            <Plus :size="18" />
            <span>新建分类</span>
          </button>
          <button v-if="activeTab === 'kb'" class="anime-btn primary" @click="showCreateKb = true">
            <Plus :size="18" />
            <span>新建知识库</span>
          </button>
        </div>
      </div>

      <div v-if="err" class="anime-error page-error" role="alert" aria-live="assertive">{{ err }}</div>

      <div class="anime-card-body">
        <div class="anime-tabs page-tabs" role="group" aria-label="知识库管理栏目">
          <button v-for="t in tabs" :key="t.key" class="anime-tab" :class="{ active: activeTab === t.key }" :aria-pressed="activeTab === t.key" @click="activeTab = t.key; loadTabData()">{{ t.label }}</button>
        </div>

        <div v-if="loading" class="anime-empty" role="status" aria-live="polite">
          <span class="anime-loader-spinner"></span>
          <span class="anime-empty-text">加载中...</span>
        </div>

        <!-- 分类管理 Tab -->
        <div v-else-if="activeTab === 'category'" class="category-section">
          <div v-if="categoryTree.length === 0" class="anime-empty">
            <div class="anime-empty-icon">📁</div>
            <div class="anime-empty-text">暂无分类，点击上方按钮创建</div>
          </div>
          <div v-else class="category-tree">
            <div v-for="cat in categoryTree" :key="cat.id" class="category-item">
              <div class="category-header">
                <div class="category-info">
                  <span class="category-icon">{{ cat.icon || '📁' }}</span>
                  <span class="category-name">{{ cat.name }}</span>
                  <span class="anime-badge blue">{{ cat.kbCount || 0 }} 知识库</span>
                </div>
                <div class="category-actions">
                  <button class="anime-btn primary sm" @click="addKbToCategory(cat)">
                    <Plus :size="14" />
                    <span>添加知识库</span>
                  </button>
                  <button v-if="cat.createBy === currentUserName" class="anime-btn ghost sm" :aria-label="`编辑分类：${cat.name}`" @click="editCategory(cat)">
                    <Edit3 :size="14" />
                  </button>
                  <button v-if="cat.createBy === currentUserName" class="anime-btn ghost sm danger" :aria-label="`删除分类：${cat.name}`" @click="deleteCategory(cat)">
                    <Trash2 :size="14" />
                  </button>
                </div>
              </div>
              <div v-if="getCategoryKbList(cat.id).length > 0" class="category-kb-list">
                <div v-for="kb in getCategoryKbList(cat.id)" :key="kb.id" class="kb-mini-card" role="group" :aria-label="`知识库：${kb.name}`">
                  <button type="button" class="kb-mini-open" :aria-label="`查看${kb.name}文档`" @click.stop="viewDocs(kb)">
                    <span class="kb-mini-name">{{ kb.name }}</span>
                    <span class="anime-badge muted">{{ kb.docCount || 0 }} 文档</span>
                  </button>
                  <button class="anime-btn ghost xs" :aria-label="`编辑知识库：${kb.name}`" @click.stop="editKb(kb)">
                    <Edit3 :size="12" />
                  </button>
                </div>
              </div>
              <div v-else class="category-kb-empty">
                <span class="empty-category-note">该分类下暂无知识库</span>
                <button class="anime-btn ghost sm" @click="addKbToCategory(cat)">
                  <Plus :size="14" />
                  <span>添加</span>
                </button>
              </div>
              <div v-if="cat.children && cat.children.length > 0" class="category-children">
                <div v-for="child in cat.children" :key="child.id" class="category-item child">
                  <div class="category-header">
                    <div class="category-info">
                      <span class="category-icon">{{ child.icon || '📂' }}</span>
                      <span class="category-name">{{ child.name }}</span>
                      <span class="anime-badge purple">{{ child.kbCount || 0 }} 知识库</span>
                    </div>
                    <div class="category-actions">
                      <button class="anime-btn primary sm" @click="addKbToCategory(child)">
                        <Plus :size="14" />
                        <span>添加</span>
                      </button>
                  <button v-if="child.createBy === currentUserName" class="anime-btn ghost sm" :aria-label="`编辑分类：${child.name}`" @click="editCategory(child)">
                        <Edit3 :size="14" />
                      </button>
                  <button v-if="child.createBy === currentUserName" class="anime-btn ghost sm danger" :aria-label="`删除分类：${child.name}`" @click="deleteCategory(child)">
                        <Trash2 :size="14" />
                      </button>
                    </div>
                  </div>
                  <div v-if="getCategoryKbList(child.id).length > 0" class="category-kb-list">
                      <div v-for="kb in getCategoryKbList(child.id)" :key="kb.id" class="kb-mini-card" role="group" :aria-label="`知识库：${kb.name}`">
                      <button type="button" class="kb-mini-open" :aria-label="`查看${kb.name}文档`" @click.stop="viewDocs(kb)">
                        <span class="kb-mini-name">{{ kb.name }}</span>
                        <span class="anime-badge muted">{{ kb.docCount || 0 }} 文档</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 知识库列表 Tab -->
        <div v-else-if="activeTab === 'kb'" class="kb-list">
          <div class="kb-sub-tabs" role="group" aria-label="知识库范围">
            <button class="anime-tab" :class="{ active: kbTab === 'mine' }" :aria-pressed="kbTab === 'mine'" @click="kbTab = 'mine'">我的知识库</button>
            <button class="anime-tab" :class="{ active: kbTab === 'all' }" :aria-pressed="kbTab === 'all'" @click="kbTab = 'all'">知识库大厅</button>
          </div>
          <div class="kb-filter-bar">
            <div class="filter-group">
              <label for="kb-category-filter" class="filter-label">筛选分类</label>
              <select id="kb-category-filter" v-model="filterCategoryId" class="filter-select" @change="filterKbList">
                <option value="">全部</option>
                <option v-for="cat in flatCategories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
              </select>
            </div>
            <div class="kb-count-info">
              共 {{ filteredKbList.length }} 个知识库
            </div>
          </div>
          
          <div v-if="filteredKbList.length === 0" class="anime-empty">
            <div class="anime-empty-icon">📚</div>
            <div class="anime-empty-text">暂无知识库</div>
          </div>
          <div v-else class="kb-grid">
            <div v-for="kb in filteredKbList" :key="kb.id" class="anime-card kb-card">
              <div class="kb-header">
                <span class="kb-name">{{ kb.name }}</span>
                <div class="kb-badges">
                  <span v-if="kb.categoryId" class="anime-badge purple">{{ getCategoryName(kb.categoryId) }}</span>
                  <span class="anime-badge blue">{{ kb.docCount || 0 }} 文档</span>
                </div>
              </div>
              <div class="kb-desc">{{ kb.description || '无描述' }}</div>
              <div class="kb-meta">
                <span class="anime-badge" :class="kb.status ? 'green' : 'pink'">{{ kb.status ? '启用' : '禁用' }}</span>
                <span v-if="kb.isPublic !== undefined" class="anime-badge" :class="kb.isPublic ? 'green' : 'gray'">{{ kb.isPublic ? '公开' : '私有' }}</span>
                <span class="anime-code">{{ kb.sourceType || '手动' }}</span>
                <span class="kb-time">创建者: {{ kb.createBy || '-' }}</span>
                <span class="kb-time">创建: {{ kb.createTime }}</span>
              </div>
              <div class="kb-actions">
                <button class="anime-btn ghost" @click="viewDocs(kb)">
                  <BookOpen :size="16" />
                  <span>文档</span>
                </button>
                <button class="anime-btn primary" @click="openUploadModal(kb)">
                  <Upload :size="16" />
                  <span>上传</span>
                </button>
                <button class="anime-btn blue" @click="syncKb(kb)">
                  <RefreshCw :size="16" />
                  <span>同步</span>
                </button>
                <button v-if="kb.createBy === currentUserName" class="anime-btn ghost" @click="editKb(kb)">
                  <Edit3 :size="16" />
                  <span>编辑</span>
                </button>
                <button v-if="kb.createBy === currentUserName" class="anime-btn ghost danger" @click="deleteKb(kb)">
                  <Trash2 :size="16" />
                  <span>删除</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 文档列表 Tab -->
        <div v-else-if="activeTab === 'doc'" class="doc-section">
          <div v-if="!selectedKb" class="anime-empty">
            <div class="anime-empty-icon">👈</div>
            <div class="anime-empty-text">请先在知识库列表中选择一个知识库查看文档</div>
          </div>
          <template v-else>
            <div class="doc-header">
              <div class="doc-title-row">
                <span class="anime-badge green">{{ selectedKb.name }}</span>
                <span v-if="selectedKb.categoryId" class="anime-badge purple">{{ getCategoryName(selectedKb.categoryId) }}</span>
                <span class="doc-heading">文档列表</span>
              </div>
              <div class="doc-actions">
                <div class="doc-search-wrapper">
                  <Search :size="16" class="doc-search-icon" />
                  <input v-model="docSearchKeyword" class="doc-search-input" type="search" aria-label="搜索文档" placeholder="搜索文档..." @input="handleDocSearch" />
                  <button v-if="docSearchKeyword" class="doc-search-clear" aria-label="清除文档搜索" @click="clearDocSearch">✕</button>
                </div>
                <button class="anime-btn primary" @click="showCreateDoc = true">
                  <Plus :size="18" />
                  <span>新增文档</span>
                </button>
              </div>
            </div>
            <div v-if="docLoading" class="anime-empty" role="status" aria-live="polite">
              <span class="anime-loader-spinner"></span>
              <span class="anime-empty-text">加载中...</span>
            </div>
            <div v-else-if="docList.length === 0" class="anime-empty">
              <div class="anime-empty-icon">📄</div>
              <div class="anime-empty-text">暂无文档</div>
            </div>
            <div v-else class="doc-table-scroll" role="region" aria-label="文档列表" tabindex="0">
            <table class="anime-table">
              <thead><tr><th>标题</th><th>状态</th><th>来源</th><th>同步</th><th>创建时间</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="doc in docList" :key="doc.id">
                  <td>{{ doc.title }}</td>
                  <td><span class="anime-badge" :class="doc.status ? 'green' : 'pink'">{{ doc.status ? '启用' : '禁用' }}</span></td>
                  <td class="anime-code">{{ doc.source }}</td>
                  <td><span class="anime-badge" :class="getSyncColor(doc.syncStatus)">{{ getSyncText(doc.syncStatus) }}</span></td>
                  <td>{{ doc.createTime }}</td>
                  <td class="doc-row-actions">
                    <button class="anime-btn blue sm" @click="syncDoc(doc)">同步</button>
                    <button class="anime-btn ghost sm danger" @click="deleteDoc(doc)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
            </div>
          </template>
        </div>

        <!-- FAQ Tab -->
        <div v-else-if="activeTab === 'faq'" class="faq-section">
          <div v-if="faqList.length === 0" class="anime-empty">
            <div class="anime-empty-icon">❓</div>
            <div class="anime-empty-text">暂无FAQ</div>
          </div>
          <div v-else class="faq-grid">
            <div v-for="faq in faqList" :key="faq.id" class="anime-card faq-card">
              <div class="faq-question">{{ faq.question }}</div>
              <div class="faq-answer">{{ faq.answer }}</div>
              <div class="faq-footer">
                <span class="anime-badge green">命中 {{ faq.hitCount || 0 }}</span>
                <button class="anime-btn ghost sm danger" @click="deleteFaq(faq)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 创建分类弹窗 -->
      <div v-if="showCreateCategory" class="anime-modal-overlay" @click.self="closeCategoryModal">
        <div ref="categoryDialogRef" class="anime-modal" role="dialog" aria-modal="true" tabindex="-1" :aria-label="editingCategory ? '编辑分类' : '新建分类'">
          <div class="anime-modal-header">
            <span>{{ editingCategory ? '编辑分类' : '新建分类' }}</span>
            <button class="anime-modal-close" aria-label="关闭分类窗口" @click="closeCategoryModal">✕</button>
          </div>
          <div class="anime-modal-body">
            <div class="anime-form-group">
              <label for="category-name">分类名称 *</label>
              <input id="category-name" v-model="formCategory.name" class="anime-input" aria-label="分类名称" placeholder="如：医疗知识" />
            </div>
            <div class="anime-form-group">
              <label for="category-icon">分类图标</label>
              <input id="category-icon" v-model="formCategory.icon" class="anime-input" aria-label="分类图标" placeholder="如：🏥、📚、💼" />
            </div>
            <div class="anime-form-group">
              <label for="category-parent">父级分类</label>
              <select id="category-parent" v-model="formCategory.parentId" class="anime-input" aria-label="父级分类">
                <option value="">无（顶级分类）</option>
                <option v-for="cat in flatCategories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
              </select>
            </div>
            <div class="anime-form-group">
              <label for="category-sort-order">排序号</label>
              <input id="category-sort-order" v-model.number="formCategory.sortOrder" class="anime-input" type="number" aria-label="排序号" placeholder="数字越小越靠前" />
            </div>
            <div class="anime-form-group">
              <label for="category-description">描述</label>
              <textarea id="category-description" v-model="formCategory.description" class="anime-input" rows="2" aria-label="分类描述" placeholder="分类描述"></textarea>
            </div>
          </div>
          <div class="anime-modal-footer">
            <button class="anime-btn ghost" @click="closeCategoryModal">取消</button>
            <button class="anime-btn primary" @click="saveCategory" :disabled="savingCategory">
              <span v-if="savingCategory">保存中...</span>
              <span v-else>{{ editingCategory ? '更新' : '创建' }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 选择知识库添加到分类弹窗 -->
      <div v-if="showAddKbToCategoryModal" class="anime-modal-overlay" @click.self="closeAddKbToCategoryDialog">
        <div ref="addKbToCategoryDialogRef" class="anime-modal" role="dialog" aria-modal="true" tabindex="-1" :aria-label="`添加知识库到分类：${selectedCategory?.name || ''}`" style="max-width: 600px;">
          <div class="anime-modal-header">
            <span>添加知识库到分类 - {{ selectedCategory?.name }}</span>
            <button class="anime-modal-close" aria-label="关闭选择知识库窗口" @click="closeAddKbToCategoryDialog">✕</button>
          </div>
          <div class="anime-modal-body">
            <div class="add-kb-search">
              <Search :size="16" />
              <input v-model="addKbSearchKeyword" class="anime-input" aria-label="搜索可添加的知识库" placeholder="搜索知识库..." />
            </div>
            <div v-if="availableKbList.length === 0" class="anime-empty">
              <div class="anime-empty-icon">📚</div>
              <div class="anime-empty-text">暂无可添加的知识库</div>
            </div>
            <div v-else class="add-kb-list">
              <div v-for="kb in filteredAvailableKbList" :key="kb.id" class="add-kb-item" :class="{ selected: selectedKbIds.includes(kb.id) }" role="checkbox" :aria-checked="selectedKbIds.includes(kb.id)" tabindex="0" @click="toggleKbSelection(kb.id)" @keydown.enter.prevent="toggleKbSelection(kb.id)" @keydown.space.prevent="toggleKbSelection(kb.id)">
                <div class="add-kb-info">
                  <span class="add-kb-name">{{ kb.name }}</span>
                  <span class="anime-badge blue">{{ kb.docCount || 0 }} 文档</span>
                  <span v-if="kb.categoryId" class="anime-badge purple">当前: {{ getCategoryName(kb.categoryId) }}</span>
                  <span v-else class="anime-badge muted">未分类</span>
                </div>
                <div class="add-kb-check">
                  <span v-if="selectedKbIds.includes(kb.id)" class="check-icon">✓</span>
                </div>
              </div>
            </div>
            <div v-if="selectedKbIds.length > 0" class="selection-info">
              已选择 {{ selectedKbIds.length }} 个知识库
            </div>
          </div>
          <div class="anime-modal-footer">
            <button class="anime-btn ghost" @click="closeAddKbToCategoryDialog">取消</button>
            <button class="anime-btn primary" :disabled="selectedKbIds.length === 0 || addingKb" @click="confirmAddKbToCategory">
              <span v-if="addingKb">添加中...</span>
              <span v-else>添加到分类</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 创建知识库弹窗 -->
      <div v-if="showCreateKb" class="anime-modal-overlay" @click.self="closeKbModal">
        <div ref="kbDialogRef" class="anime-modal" role="dialog" aria-modal="true" tabindex="-1" :aria-label="editingKb ? '编辑知识库' : '新建知识库'">
          <div class="anime-modal-header">
            <span>{{ editingKb ? '编辑知识库' : '新建知识库' }}</span>
            <button class="anime-modal-close" aria-label="关闭知识库窗口" @click="closeKbModal">✕</button>
          </div>
          <div class="anime-modal-body">
            <div class="anime-form-group">
              <label for="kb-name">知识库名称 *</label>
              <input id="kb-name" v-model="formKb.name" class="anime-input" aria-label="知识库名称" placeholder="知识库名称" />
            </div>
            <div class="anime-form-group">
              <label for="kb-category">所属分类</label>
              <select id="kb-category" v-model="formKb.categoryId" class="anime-input" aria-label="所属分类">
                <option value="">无分类</option>
                <option v-for="cat in flatCategories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
              </select>
            </div>
            <div class="anime-form-group">
              <label for="kb-description">描述</label>
              <textarea id="kb-description" v-model="formKb.description" class="anime-input" rows="2" aria-label="知识库描述" placeholder="知识库描述"></textarea>
            </div>
            <div class="anime-form-group">
              <label style="display: flex; align-items: center; gap: 8px;">
                <input type="checkbox" v-model="formKb.isPublic" aria-label="公开知识库" />
                公开知识库（其他用户可使用）
              </label>
            </div>
          </div>
          <div class="anime-modal-footer">
            <button class="anime-btn ghost" @click="closeKbModal">取消</button>
            <button class="anime-btn primary" @click="saveKb" :disabled="savingKb">
              <span v-if="savingKb">保存中...</span>
              <span v-else>{{ editingKb ? '更新' : '创建' }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 创建文档弹窗 -->
      <div v-if="showCreateDoc" class="anime-modal-overlay" @click.self="closeCreateDocDialog">
        <div ref="docDialogRef" class="anime-modal" role="dialog" aria-modal="true" tabindex="-1" aria-label="新增文档">
          <div class="anime-modal-header">
            <span>新增文档</span>
            <button class="anime-modal-close" aria-label="关闭新增文档窗口" @click="closeCreateDocDialog">✕</button>
          </div>
          <div class="anime-modal-body">
            <div class="anime-form-group">
              <label for="document-title">标题 *</label>
              <input id="document-title" v-model="formDoc.title" class="anime-input" aria-label="文档标题" placeholder="文档标题" />
            </div>
            <div class="anime-form-group">
              <label for="document-content">内容</label>
              <textarea id="document-content" v-model="formDoc.content" class="anime-input" rows="4" aria-label="文档内容" placeholder="文档内容"></textarea>
            </div>
          </div>
          <div class="anime-modal-footer">
            <button class="anime-btn primary" @click="createDocSubmit">确定</button>
            <button class="anime-btn ghost" @click="closeCreateDocDialog">取消</button>
          </div>
        </div>
      </div>

      <!-- 批量上传弹窗 -->
      <div v-if="showUploadModal" class="anime-modal-overlay" @click.self="closeUploadDialog">
        <div ref="uploadDialogRef" class="anime-modal" role="dialog" aria-modal="true" tabindex="-1" aria-label="批量上传文件到知识库" style="max-width: 600px;">
          <div class="anime-modal-header">
            <span>批量上传文件到知识库</span>
            <button class="anime-modal-close" aria-label="关闭批量上传窗口" @click="closeUploadDialog">✕</button>
          </div>
          <div class="anime-modal-body">
            <div style="margin-bottom: 16px;">
              <input ref="uploadInputRef" type="file" multiple aria-label="选择要上传的知识库文件" style="display: none;" :disabled="uploadLoading" @change="handleUploadFileSelect" />
              <button class="anime-btn primary" @click="triggerUploadInput">
                <Upload :size="18" />
                <span>选择文件</span>
              </button>
              <span class="upload-hint">支持 Word、PDF、Markdown 等</span>
            </div>
            <div v-if="uploadFiles.length > 0" style="margin-bottom: 16px;">
              <div class="upload-files-label">已选择 {{ uploadFiles.length }} 个文件:</div>
              <div style="display: flex; flex-direction: column; gap: 8px; max-height: 200px; overflow-y: auto;">
                <div v-for="(f, i) in uploadFiles" :key="i" class="upload-file-row">
                  <span class="upload-file-name">{{ f.name }}</span>
                  <span class="anime-code" style="font-size: 12px;">{{ formatSize(f.size) }}</span>
                  <button class="anime-btn ghost sm" :aria-label="`移除文件：${f.name}`" @click="removeUploadFile(i)">✕</button>
                </div>
              </div>
            </div>
            <div v-if="uploadProgress && uploadLoading" class="upload-progress" role="status" aria-live="polite">
              <div class="upload-progress-heading">
                <span class="anime-badge pink">{{ uploadProgress.status === 'completed' ? '完成' : '上传中' }}</span>
                <span class="upload-progress-count">{{ uploadProgress.completedCount }} / {{ uploadProgress.totalCount }}</span>
              </div>
              <div class="anime-progress" role="progressbar" aria-label="文件上传进度" :aria-valuenow="uploadProgress.progressPercent" aria-valuemin="0" aria-valuemax="100" style="margin-bottom: 12px;">
                <div class="anime-progress-bar" :style="{ width: uploadProgress.progressPercent + '%' }"></div>
              </div>
              <div v-if="uploadProgress.currentFile" class="upload-current-file">
                当前: {{ uploadProgress.currentFile }}
              </div>
            </div>
            <div v-if="uploadResult && !uploadLoading" class="upload-success" role="status" aria-live="polite">
              <div>上传完成: 成功 {{ uploadResult.successCount }} / {{ uploadResult.totalCount }}</div>
            </div>
          </div>
          <div class="anime-modal-footer">
            <button class="anime-btn primary" :disabled="uploadLoading || uploadFiles.length === 0" @click="doBatchUpload">
              <Upload :size="18" />
              <span v-if="uploadLoading">上传中...</span>
              <span v-else>开始上传</span>
            </button>
            <button class="anime-btn ghost" @click="closeUploadDialog">关闭</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useModalDialog } from '../composables/useModalDialog'
import { Plus, BookOpen, RefreshCw, Edit3, Trash2, Upload, Download, Search } from 'lucide-vue-next'
import { api } from '../api/client'
import { batchUploadToKb, syncFromDify } from '../api/kb'
import { subscribeUploadProgress } from '../api/progress'
import type { UploadProgress } from '../api/progress'
import type { KbKnowledgeBase, KbDocument } from '../api/kb'
import type { BatchUploadResponse } from '../types/dify'
import { getOrCreateUserId } from '../lib/user'

const userId = getOrCreateUserId()

const currentUserName = computed(() => localStorage.getItem('chatbase_original_username') || localStorage.getItem('chatbase_user') || '')
const kbTab = ref<'mine' | 'all'>('mine')

interface KbCategory {
  id: number
  name: string
  icon: string
  parentId: number | null
  sortOrder: number
  description: string
  kbCount?: number
  createBy?: string
  children?: KbCategory[]
}

const tabs = [
  { key: 'category' as const, label: '分类' },
  { key: 'kb' as const, label: '知识库' },
  { key: 'doc' as const, label: '文档' },
  { key: 'faq' as const, label: 'FAQ' }
]
const activeTab = ref<'category' | 'kb' | 'doc' | 'faq'>('category')
const loading = ref(false)
const syncing = ref(false)
const err = ref<string | null>(null)

const categoryTree = ref<KbCategory[]>([])
const flatCategories = computed(() => {
  const flat: KbCategory[] = []
  const flatten = (items: KbCategory[]) => {
    items.forEach(item => {
      flat.push(item)
      if (item.children?.length) flatten(item.children)
    })
  }
  flatten(categoryTree.value)
  return flat
})

const kbList = ref<KbKnowledgeBase[]>([])
const filterCategoryId = ref<string>('')
const filteredKbList = computed(() => {
  let list = kbList.value
  if (kbTab.value === 'mine') {
    list = list.filter(kb => kb.createBy === currentUserName.value)
  } else if (kbTab.value === 'all') {
    list = list.filter(kb => kb.isPublic)
  }
  if (filterCategoryId.value) {
    list = list.filter(kb => kb.categoryId === Number(filterCategoryId.value))
  }
  return list
})

const docList = ref<KbDocument[]>([])
const faqList = ref<any[]>([])
const selectedKb = ref<KbKnowledgeBase | null>(null)
const docLoading = ref(false)
const docSearchKeyword = ref('')
const docSearchTimer = ref<number | null>(null)

const showCreateCategory = ref(false)
const showCreateKb = ref(false)
const showCreateDoc = ref(false)
const showUploadModal = ref(false)
const showAddKbToCategoryModal = ref(false)
const closeAddKbToCategoryDialog = () => { showAddKbToCategoryModal.value = false }
const closeCreateDocDialog = () => { showCreateDoc.value = false }
const closeUploadDialog = () => { showUploadModal.value = false }
const { dialogRef: categoryDialogRef } = useModalDialog(showCreateCategory, closeCategoryModal)
const { dialogRef: addKbToCategoryDialogRef } = useModalDialog(showAddKbToCategoryModal, closeAddKbToCategoryDialog)
const { dialogRef: kbDialogRef } = useModalDialog(showCreateKb, closeKbModal)
const { dialogRef: docDialogRef } = useModalDialog(showCreateDoc, closeCreateDocDialog)
const { dialogRef: uploadDialogRef } = useModalDialog(showUploadModal, closeUploadDialog)
const selectedCategory = ref<KbCategory | null>(null)
const addKbSearchKeyword = ref('')
const selectedKbIds = ref<number[]>([])
const addingKb = ref(false)
const availableKbList = ref<KbKnowledgeBase[]>([])

const filteredAvailableKbList = computed(() => {
  const keyword = addKbSearchKeyword.value.toLowerCase().trim()
  if (!keyword) return availableKbList.value
  return availableKbList.value.filter(kb => kb.name.toLowerCase().includes(keyword))
})

const editingCategory = ref<KbCategory | null>(null)
const editingKb = ref<KbKnowledgeBase | null>(null)

const formCategory = ref({ name: '', icon: '', parentId: '', sortOrder: 0, description: '' })
const formKb = ref({ name: '', categoryId: '', description: '', isPublic: true })
const formDoc = ref({ title: '', content: '' })

const savingCategory = ref(false)
const savingKb = ref(false)

const uploadFiles = ref<File[]>([])
const uploadLoading = ref(false)
const uploadResult = ref<BatchUploadResponse | null>(null)
const uploadKbId = ref<number | null>(null)
const uploadInputRef = ref<HTMLInputElement | null>(null)
const uploadProgress = ref<UploadProgress | null>(null)
const uploadEventSource = ref<EventSource | null>(null)

async function loadCategoryTree() {
  try {
    const res = await api.get('/kb/category/tree')
    categoryTree.value = res.data || []
  } catch (e) {
    console.error('加载分类失败', e)
    categoryTree.value = []
  }
}

async function loadKbList() {
  loading.value = true
  err.value = null
  try {
    const res = await api.get('/kb/page', { params: { pageNum: 1, pageSize: 100 } })
    kbList.value = res.data.records || []
  } catch (e: any) { err.value = e?.message || '加载失败' }
  finally { loading.value = false }
}

function filterKbList() {}

function getCategoryName(id: number | null) {
  if (!id) return '无分类'
  const cat = flatCategories.value.find(c => c.id === id)
  return cat?.name || '未知'
}

function getCategoryKbList(catId: number) {
  return kbList.value.filter(kb => kb.categoryId === catId)
}

async function doSyncFromDify() {
  syncing.value = true
  err.value = null
  try {
    const result = await syncFromDify()
    if (result.success) {
      await Promise.all([loadKbList(), loadCategoryTree()])
    } else {
      err.value = result.message
    }
  } catch (e: any) {
    err.value = e?.message || '同步失败'
  } finally {
    syncing.value = false
  }
}

async function loadDocList(kbId: number) {
  docLoading.value = true
  try {
    const keyword = docSearchKeyword.value.trim()
    const res = await api.get(`/kb/${kbId}/document/page`, { 
      params: { pageNum: 1, pageSize: 100, title: keyword || undefined } 
    })
    docList.value = res.data.records || []
  } finally { docLoading.value = false }
}

function handleDocSearch() {
  if (docSearchTimer.value) clearTimeout(docSearchTimer.value)
  docSearchTimer.value = window.setTimeout(() => {
    if (selectedKb.value) loadDocList(selectedKb.value.id)
  }, 300)
}

function clearDocSearch() {
  docSearchKeyword.value = ''
  if (selectedKb.value) loadDocList(selectedKb.value.id)
}

async function loadFaqList() {
  try {
    const res = await api.get('/kb/conversation/faq/page', { params: { pageNum: 1, pageSize: 100 } })
    faqList.value = res.data.records || []
  } catch { faqList.value = [] }
}

function loadTabData() {
  if (activeTab.value === 'category') {
    if (categoryTree.value.length === 0) loadCategoryTree()
    if (kbList.value.length === 0) loadKbList()
  }
  else if (activeTab.value === 'kb' && kbList.value.length === 0) loadKbList()
  else if (activeTab.value === 'faq') loadFaqList()
}

async function saveCategory() {
  if (!formCategory.value.name) {
    err.value = '请输入分类名称'
    return
  }
  savingCategory.value = true
  try {
    const payload = {
      name: formCategory.value.name,
      icon: formCategory.value.icon,
      parentId: formCategory.value.parentId ? Number(formCategory.value.parentId) : null,
      sortOrder: formCategory.value.sortOrder || 0,
      description: formCategory.value.description
    }
    if (editingCategory.value) {
      await api.put('/kb/category', { ...payload, id: editingCategory.value.id })
    } else {
      await api.post('/kb/category', payload)
    }
    closeCategoryModal()
    loadCategoryTree()
  } catch (e: any) {
    err.value = e.response?.data?.message || '保存失败'
  } finally {
    savingCategory.value = false
  }
}

function editCategory(cat: KbCategory) {
  editingCategory.value = cat
  formCategory.value = {
    name: cat.name,
    icon: cat.icon || '',
    parentId: cat.parentId ? String(cat.parentId) : '',
    sortOrder: cat.sortOrder || 0,
    description: cat.description || ''
  }
  showCreateCategory.value = true
}

async function deleteCategory(cat: KbCategory) {
  if (cat.kbCount && cat.kbCount > 0) {
    alert('该分类下有知识库，无法删除')
    return
  }
  if (!confirm(`确定删除分类 "${cat.name}"？`)) return
  try {
    const res = await api.delete(`/kb/category/${cat.id}`)
    if (res.data && res.data.success === false) {
      err.value = res.data.message || '删除失败'
      return
    }
    loadCategoryTree()
  } catch (e: any) {
    err.value = e.response?.data?.message || '删除失败'
  }
}

function closeCategoryModal() {
  showCreateCategory.value = false
  editingCategory.value = null
  formCategory.value = { name: '', icon: '', parentId: '', sortOrder: 0, description: '' }
  err.value = ''
}

function viewCategoryKb(cat: KbCategory) {
  filterCategoryId.value = String(cat.id)
  activeTab.value = 'kb'
}

function addKbToCategory(cat: KbCategory) {
  selectedCategory.value = cat
  addKbSearchKeyword.value = ''
  selectedKbIds.value = []
  availableKbList.value = kbList.value
  showAddKbToCategoryModal.value = true
}

function toggleKbSelection(kbId: number) {
  const idx = selectedKbIds.value.indexOf(kbId)
  if (idx >= 0) {
    selectedKbIds.value.splice(idx, 1)
  } else {
    selectedKbIds.value.push(kbId)
  }
}

async function confirmAddKbToCategory() {
  if (selectedKbIds.value.length === 0 || !selectedCategory.value) return
  addingKb.value = true
  try {
    for (const kbId of selectedKbIds.value) {
      await api.put('/kb', { id: kbId, categoryId: selectedCategory.value.id })
    }
    showAddKbToCategoryModal.value = false
    await Promise.all([loadKbList(), loadCategoryTree()])
  } catch (e: any) {
    err.value = e.response?.data?.message || '添加失败'
  } finally {
    addingKb.value = false
  }
}

async function saveKb() {
  if (!formKb.value.name) {
    err.value = '请输入知识库名称'
    return
  }
  savingKb.value = true
  try {
    const payload = {
      name: formKb.value.name,
      categoryId: formKb.value.categoryId ? Number(formKb.value.categoryId) : null,
      description: formKb.value.description,
      isPublic: formKb.value.isPublic
    }
    if (editingKb.value) {
      await api.put('/kb', { ...payload, id: editingKb.value.id })
    } else {
      await api.post('/kb', payload)
    }
    closeKbModal()
    loadKbList()
  } catch (e: any) {
    err.value = e.response?.data?.message || '保存失败'
  } finally {
    savingKb.value = false
  }
}

function editKb(kb: KbKnowledgeBase) {
  editingKb.value = kb
  formKb.value = {
    name: kb.name,
    categoryId: kb.categoryId ? String(kb.categoryId) : '',
    description: kb.description || '',
    isPublic: kb.isPublic !== undefined ? kb.isPublic : true
  }
  showCreateKb.value = true
}

async function deleteKb(kb: KbKnowledgeBase) {
  if (!confirm(`确定删除知识库 "${kb.name}"？`)) return
  try {
    await api.delete(`/kb/${kb.id}`)
    loadKbList()
  } catch (e: any) {
    err.value = e.response?.data?.message || '删除失败'
  }
}

function closeKbModal() {
  showCreateKb.value = false
  editingKb.value = null
  formKb.value = { name: '', categoryId: '', description: '', isPublic: true }
  err.value = ''
}

function viewDocs(kb: KbKnowledgeBase) {
  selectedKb.value = kb
  docSearchKeyword.value = ''
  activeTab.value = 'doc'
  loadDocList(kb.id)
}

async function syncKb(kb: KbKnowledgeBase) {
  try {
    const res = await api.post(`/kb/${kb.id}/sync`)
    if (res.data?.success) alert('同步成功')
    else alert(res.data?.message || '同步失败')
  } catch { alert('同步失败') }
}

async function createDocSubmit() {
  if (!selectedKb.value || !formDoc.value.title) return
  try {
    await api.post('/kb/document', { ...formDoc.value, knowledgeBaseId: selectedKb.value.id })
    showCreateDoc.value = false
    formDoc.value = { title: '', content: '' }
    loadDocList(selectedKb.value.id)
  } catch (e: any) {
    err.value = e?.message || '创建失败'
  }
}

async function deleteDoc(doc: KbDocument) {
  if (!confirm(`确定删除文档 "${doc.title}"？`)) return
  try {
    await api.delete(`/kb/document/${doc.id}`)
    if (selectedKb.value) loadDocList(selectedKb.value.id)
  } catch (e: any) {
    err.value = e?.message || '删除失败'
  }
}

async function syncDoc(doc: KbDocument) {
  try {
    const res = await api.post(`/kb/document/${doc.id}/sync`)
    if (res.data?.success) alert('同步成功')
    else alert(res.data?.message || '同步失败')
  } catch { alert('同步失败') }
}

async function deleteFaq(faq: any) {
  if (!confirm('确定删除FAQ？')) return
  try {
    await api.delete(`/kb/conversation/faq/${faq.id}`)
    loadFaqList()
  } catch (e: any) {
    err.value = e?.message || '删除失败'
  }
}

function getSyncColor(status: number): string {
  if (status === 1) return 'green'
  if (status === 2) return 'pink'
  return 'muted'
}

function getSyncText(status: number): string {
  if (status === 1) return '已同步'
  if (status === 2) return '失败'
  return '未同步'
}

function openUploadModal(kb: KbKnowledgeBase) {
  uploadKbId.value = kb.id
  uploadFiles.value = []
  uploadResult.value = null
  uploadProgress.value = null
  showUploadModal.value = true
}

function triggerUploadInput() {
  uploadInputRef.value?.click()
}

function handleUploadFileSelect(e: Event) {
  const el = e.target as HTMLInputElement
  const files = el.files
  if (files && files.length > 0) {
    uploadFiles.value = [...uploadFiles.value, ...Array.from(files)]
  }
  el.value = ''
}

function removeUploadFile(idx: number) {
  uploadFiles.value.splice(idx, 1)
}

async function doBatchUpload() {
  if (uploadFiles.value.length === 0 || !uploadKbId.value) return
  uploadLoading.value = true
  uploadResult.value = null
  uploadProgress.value = null
  
  const form = new FormData()
  uploadFiles.value.forEach(f => form.append('files', f))
  form.append('user', userId)
  
  try {
    const resp = await api.post<{ success: boolean; taskId: string; message: string }>(`/kb/${uploadKbId.value}/batch-upload`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    if (!resp.data.success || !resp.data.taskId) {
      err.value = resp.data.message || '创建上传任务失败'
      uploadLoading.value = false
      return
    }
    
    const taskId = resp.data.taskId
    uploadProgress.value = {
      taskId,
      totalCount: uploadFiles.value.length,
      completedCount: 0,
      successCount: 0,
      failedCount: 0,
      currentFile: '准备上传...',
      status: 'pending',
      fileProgresses: [],
      progressPercent: 0,
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    }
    
    uploadEventSource.value = subscribeUploadProgress(
      taskId,
      (progress) => { uploadProgress.value = progress },
      (progress) => {
        uploadProgress.value = progress
        uploadLoading.value = false
        uploadResult.value = {
          totalCount: progress.totalCount,
          successCount: progress.successCount,
          failedCount: progress.failedCount,
          results: progress.fileProgresses.map(fp => ({
            fileName: fp.fileName,
            success: fp.status === 'success',
            message: fp.message,
            difyFileId: fp.difyFileId || undefined
          }))
        }
        uploadFiles.value = []
        if (selectedKb.value) loadDocList(selectedKb.value.id)
      },
      (errorMsg) => {
        err.value = errorMsg
        uploadLoading.value = false
      }
    )
  } catch (e: any) {
    err.value = e?.message || '上传失败'
    uploadLoading.value = false
  }
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

onMounted(async () => {
  await Promise.all([loadCategoryTree(), loadKbList()])
})

onUnmounted(() => {
  if (uploadEventSource.value) uploadEventSource.value.close()
})
</script>

<style scoped>
.category-section { min-height: 300px; }
.category-tree { display: flex; flex-direction: column; gap: 16px; }
.category-item {
  background: var(--anime-bg-card);
  box-shadow: var(--anime-shadow-sm);
  border-radius: var(--anime-radius-lg);
  padding: 16px;
}
.category-item.child {
  margin-left: 24px;
  background: var(--ui-surface-muted);
  box-shadow: var(--ui-shadow-card);
}
.category-header { display: flex; align-items: center; justify-content: space-between; }
.category-info { display: flex; align-items: center; gap: 10px; }
.category-icon { font-size: 20px; }
.category-name { font-size: 16px; font-weight: 600; color: var(--ui-text); }
.category-actions { display: flex; gap: 6px; }
.category-children { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.category-kb-list {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--anime-border-light);
  display: flex; flex-wrap: wrap; gap: 8px;
}
.kb-mini-card {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px;
  background: var(--ui-primary-soft);
  border-radius: var(--anime-radius-lg);
  transition: all 0.2s;
}
.kb-mini-card:hover { background: var(--ui-primary-soft); transform: translateY(-1px); }
.kb-mini-name { font-size: 14px; color: var(--anime-text-primary); }

.category-kb-empty {
  margin-top: 12px;
  padding: 12px;
  border-top: 1px solid var(--anime-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.add-kb-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px 14px;
  background: var(--anime-bg);
  box-shadow: var(--anime-shadow-sm);
  border-radius: var(--anime-radius-lg);
}

.add-kb-list {
  max-height: 400px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.add-kb-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--anime-bg);
  box-shadow: var(--anime-shadow-sm);
  border-radius: var(--anime-radius-lg);
  cursor: pointer;
  transition: all 0.2s;
}

.add-kb-item:hover {
  box-shadow: var(--ui-shadow-card);
  background: var(--ui-surface-muted);
}

.add-kb-item.selected {
  box-shadow: var(--anime-shadow-pink);
  background: var(--ui-primary-soft);
}

.add-kb-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.add-kb-name {
  font-weight: 600;
  color: var(--anime-text-primary);
}

.add-kb-check {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ui-primary);
  color: white;
  border-radius: 50%;
  font-size: 14px;
}

.selection-info {
  margin-top: 12px;
  padding: 10px;
  background: var(--ui-primary-soft);
  border-radius: var(--anime-radius-lg);
  font-size: 13px;
  color: var(--ui-primary);
  font-weight: 600;
}

.kb-filter-bar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--ui-surface-muted);
  border-radius: var(--anime-radius-lg);
}
.filter-group { display: flex; align-items: center; gap: 10px; }
.filter-select {
  padding: 6px 12px;
  background: var(--anime-bg);
  box-shadow: var(--anime-shadow-sm);
  border: none;
  border-radius: var(--anime-radius-lg);
  font-size: 14px;
  color: var(--anime-text-primary);
  outline: none;
}
.filter-select:focus { box-shadow: var(--anime-shadow-pink); }
.kb-count-info { font-size: 13px; color: var(--anime-text-muted); }

.kb-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.kb-card { padding: 20px; }
.kb-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; flex-wrap: wrap; gap: 8px; }
.kb-name { font-size: 18px; font-weight: 700; color: var(--ui-text); }
.kb-badges { display: flex; gap: 6px; }
.kb-desc { font-size: 14px; color: var(--anime-text-muted); margin-bottom: 12px; }
.kb-meta { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
.kb-time { font-size: 13px; color: var(--anime-text-muted); }
.kb-actions { display: flex; gap: 10px; flex-wrap: wrap; }

.doc-section { min-height: 300px; }
.doc-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.doc-title-row { display: flex; align-items: center; gap: 10px; }
.doc-actions { display: flex; gap: 12px; }

.doc-search-wrapper {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: var(--anime-bg);
  box-shadow: var(--anime-shadow-sm); border-radius: var(--anime-radius-lg);
}
.doc-search-wrapper:focus-within { box-shadow: var(--anime-shadow-md); }
.doc-search-icon { color: var(--anime-text-muted); }
.doc-search-input { border: none; background: transparent; font-size: 13px; color: var(--anime-text-primary); outline: none; width: 150px; }
.doc-search-input::placeholder { color: var(--anime-text-muted); }
.doc-search-clear { background: transparent; border: none; color: var(--anime-text-muted); cursor: pointer; padding: 2px 6px; font-size: 12px; }
.doc-search-clear:hover { color: var(--ui-primary); }

.faq-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.faq-card { padding: 20px; }
.faq-question { font-size: 16px; font-weight: 600; color: var(--ui-text); margin-bottom: 12px; }
.faq-answer { font-size: 14px; color: var(--anime-text-secondary); line-height: 1.6; margin-bottom: 16px; }
.faq-footer { display: flex; justify-content: space-between; align-items: center; }

.anime-btn.sm { padding: 4px 8px; font-size: 12px; }
.anime-btn.xs { padding: 2px 6px; font-size: 11px; }
</style>

<style scoped>
.page-error { margin: 0 28px 16px; }
.anime-card-title { margin: 0; }
.anime-card-desc { margin: 4px 0 0; }
.page-tabs { margin-bottom: 18px; }
.kb-sub-tabs { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.category-section, .doc-section { min-height: 300px; }
.category-tree { display: flex; flex-direction: column; gap: 14px; }
.category-item {
  min-width: 0;
  padding: 18px;
  background: var(--ui-surface);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  box-shadow: var(--ui-shadow-card);
}
.category-item.child {
  margin-left: 20px;
  background: var(--ui-surface-muted);
  box-shadow: none;
}
.category-header, .category-info, .category-actions { display: flex; align-items: center; }
.category-header { justify-content: space-between; gap: 14px; }
.category-info { min-width: 0; flex-wrap: wrap; gap: 10px; }
.category-icon { font-size: 20px; }
.category-name { color: var(--ui-text); font-size: 16px; font-weight: 700; overflow-wrap: anywhere; }
.category-actions { flex-wrap: wrap; justify-content: flex-end; gap: 6px; }
.category-children { display: flex; flex-direction: column; gap: 10px; margin-top: 14px; }
.category-kb-list, .add-kb-info { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.category-kb-list { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--ui-border); }
.kb-mini-card {
  display: flex; align-items: center; gap: 8px; max-width: 100%; padding: 8px 10px;
  background: var(--ui-primary-soft); border: 1px solid var(--ui-primary-border); border-radius: var(--ui-radius-md);
  transition: background-color .15s ease, border-color .15s ease;
}
.kb-mini-card:hover, .kb-mini-card:focus-visible { background: var(--ui-surface); border-color: var(--ui-primary); }
.kb-mini-name { color: var(--ui-text); font-size: 14px; overflow-wrap: anywhere; }
.category-kb-empty { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 14px; padding: 12px; border-top: 1px solid var(--ui-border); color: var(--ui-text-secondary); }
.empty-category-note { color: var(--ui-text-secondary); font-size: 13px; }
.kb-mini-open { display: flex; align-items: center; gap: 8px; min-width: 0; padding: 0; border: 0; background: transparent; color: inherit; cursor: pointer; text-align: left; }
.kb-mini-open:focus-visible { outline: 2px solid var(--ui-focus); outline-offset: 3px; border-radius: var(--ui-radius-sm); }
.add-kb-search { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.add-kb-list { max-height: 400px; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.add-kb-item { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 14px; background: var(--ui-surface); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); cursor: pointer; }
.add-kb-item:hover, .add-kb-item:focus-visible { border-color: var(--ui-primary); outline: 2px solid transparent; }
.add-kb-item.selected { background: var(--ui-primary-soft); border-color: var(--ui-primary-border); }
.add-kb-name { color: var(--ui-text); font-weight: 600; overflow-wrap: anywhere; }
.add-kb-check { display: grid; place-items: center; flex: 0 0 24px; width: 24px; height: 24px; color: #fff; background: var(--ui-primary); border-radius: 50%; }
.selection-info { margin-top: 12px; padding: 10px 12px; background: var(--ui-primary-soft); border-radius: var(--ui-radius-md); color: var(--ui-primary-hover); font-size: 13px; font-weight: 600; }
.kb-filter-bar { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-bottom: 16px; padding: 12px 14px; background: var(--ui-surface-muted); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); }
.filter-group { display: flex; align-items: center; gap: 10px; }
.filter-label { color: var(--ui-text-secondary); font-size: 13px; font-weight: 600; }
.filter-select { min-height: 38px; padding: 6px 10px; color: var(--ui-text); background: var(--ui-surface); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-sm); }
.filter-select:focus-visible, .doc-search-wrapper:focus-within { outline: 3px solid color-mix(in srgb, var(--ui-focus) 22%, transparent); border-color: var(--ui-focus); }
.kb-count-info { color: var(--ui-text-secondary); font-size: 13px; }
.kb-grid, .faq-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr)); gap: 16px; }
.kb-card, .faq-card { min-width: 0; padding: 18px; background: var(--ui-surface); border: 1px solid var(--ui-border); box-shadow: var(--ui-shadow-card); }
.kb-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; margin-bottom: 10px; }
.kb-name { color: var(--ui-text); font-size: 17px; font-weight: 700; overflow-wrap: anywhere; }
.kb-badges, .kb-meta { display: flex; flex-wrap: wrap; gap: 6px; }
.kb-desc { margin-bottom: 12px; color: var(--ui-text-secondary); font-size: 14px; line-height: 1.55; overflow-wrap: anywhere; }
.kb-meta { align-items: center; margin-bottom: 14px; }
.kb-time { color: var(--ui-text-secondary); font-size: 12px; }
.kb-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.doc-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; margin-bottom: 16px; }
.doc-title-row, .doc-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.doc-title-row .doc-heading { color: var(--ui-text); font-weight: 700; }
.doc-search-wrapper { display: flex; align-items: center; gap: 8px; min-height: 40px; padding: 6px 10px; background: var(--ui-surface); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-sm); }
.doc-search-icon { color: var(--ui-text-muted); }
.doc-search-input { width: min(220px, 45vw); border: 0; outline: 0; background: transparent; color: var(--ui-text); }
.doc-search-input::placeholder { color: var(--ui-text-muted); }
.doc-search-clear { min-width: 30px; min-height: 30px; border: 0; border-radius: var(--ui-radius-sm); background: transparent; color: var(--ui-text-secondary); cursor: pointer; }
.doc-search-clear:hover { background: var(--ui-surface-muted); color: var(--ui-text); }
.doc-table-scroll { max-width: 100%; overflow-x: auto; border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); }
.doc-table-scroll .anime-table { min-width: 720px; }
.doc-row-actions { white-space: nowrap; }
.upload-hint, .upload-files-label, .upload-current-file { color: var(--ui-text-secondary); font-size: 13px; }
.upload-hint { margin-left: 12px; }
.upload-files-label { margin-bottom: 8px; }
.upload-file-row { display: flex; align-items: center; gap: 8px; padding: 6px 10px; background: var(--ui-surface-muted); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-sm); }
.upload-file-name { flex: 1; min-width: 0; overflow: hidden; color: var(--ui-text); font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.upload-progress { margin-bottom: 16px; padding: 16px; background: var(--ui-primary-soft); border: 1px solid var(--ui-primary-border); border-radius: var(--ui-radius-md); }
.upload-progress-heading { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.upload-progress-count { color: var(--ui-text); font-weight: 650; }
.upload-current-file { overflow-wrap: anywhere; }
.upload-success { padding: 12px; background: var(--ui-success-soft); border: 1px solid var(--ui-success-border); border-radius: var(--ui-radius-md); color: var(--ui-success); font-weight: 650; }
.faq-question { color: var(--ui-text); font-size: 16px; font-weight: 700; margin-bottom: 10px; }
.faq-answer { color: var(--ui-text-secondary); font-size: 14px; line-height: 1.6; margin-bottom: 14px; overflow-wrap: anywhere; }
.faq-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.anime-page-shell .anime-badge.blue, .anime-page-shell .anime-badge.purple { background: var(--ui-surface-muted); color: var(--ui-text-secondary); border: 1px solid var(--ui-border); }
.anime-page-shell .anime-badge.green { background: var(--ui-success-soft); color: var(--ui-success); border: 1px solid var(--ui-success-border); }
.anime-page-shell .anime-badge.pink { background: var(--ui-danger-soft); color: var(--ui-danger); border: 1px solid var(--ui-danger-border); }
.anime-page-shell .anime-badge.muted, .anime-page-shell .anime-badge.gray { background: var(--ui-surface-muted); color: var(--ui-text-secondary); border: 1px solid var(--ui-border); }
.anime-btn.sm { min-height: 34px; padding: 6px 10px; font-size: 12px; }
.anime-btn.xs { min-height: 30px; padding: 4px 8px; font-size: 11px; }
@media (max-width: 760px) {
  .page-error { margin: 0 16px 14px; }
  .category-item { padding: 14px; }
  .category-header, .doc-header, .kb-filter-bar { align-items: stretch; flex-direction: column; }
  .category-item.child { margin-left: 10px; }
  .category-actions, .doc-actions { justify-content: flex-start; }
  .doc-actions > *, .doc-search-wrapper { width: 100%; }
  .doc-search-input { width: 100%; }
  .kb-header { flex-direction: column; }
  .kb-filter-bar { gap: 10px; }
  .filter-group { align-items: flex-start; flex-direction: column; }
  .filter-select { width: 100%; }
  .kb-actions .anime-btn { flex: 1 1 auto; }
}
</style>
