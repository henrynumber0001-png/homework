<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ReasonDialog from '@/components/ReasonDialog.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  actOnCommunityComment,
  actOnCommunityPost,
  listCommunityComments,
  listCommunityPosts,
} from '@/api/admin'
import { showApiError } from '@/api/http'
import {
  CommunityContentAction,
  HitPostStatus,
  type CommunityComment,
  type CommunityContentAction as CommunityContentActionValue,
  type CommunityPost,
  type HitPostStatus as HitPostStatusValue,
} from '@/types/admin'
import { formatDateTime } from '@/utils/format'
import { communityStatusNames } from '@/utils/dictionaries'

const activeTab = ref('posts')
const loading = ref(false)
const posts = ref<CommunityPost[]>([])
const comments = ref<CommunityComment[]>([])
const total = ref(0)
const reasonDialog = ref<InstanceType<typeof ReasonDialog>>()
const pending = ref<{
  type: 'post' | 'comment'
  id: number
  action: CommunityContentActionValue
  version: number
} | null>(null)
const query = reactive({
  keyword: '',
  userId: '',
  status: '' as HitPostStatusValue | '',
  pageNum: 1,
  pageSize: 20,
})

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    const params = {
      keyword: query.keyword || undefined,
      userId: query.userId ? Number(query.userId) : undefined,
      status: query.status || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    }
    if (activeTab.value === 'posts') {
      const result = await listCommunityPosts(params)
      posts.value = result.records
      total.value = result.total
    } else {
      const result = await listCommunityComments(params)
      comments.value = result.records
      total.value = result.total
    }
  } catch (error) {
    showApiError(error)
  } finally {
    loading.value = false
  }
}

function switchTab(): void {
  query.pageNum = 1
  query.keyword = ''
  void load()
}

function search(): void {
  query.pageNum = 1
  void load()
}

function openAction(
  type: 'post' | 'comment',
  row: CommunityPost | CommunityComment,
  action: CommunityContentAction,
  title: string,
): void {
  pending.value = { type, id: row.id, action, version: row.version }
  reasonDialog.value?.open({ title, description: `内容 ID：${row.id}` })
}

async function confirmAction(reason: string): Promise<void> {
  if (!pending.value) return
  try {
    const payload = {
      action: pending.value.action,
      reason,
      version: pending.value.version,
    }
    if (pending.value.type === 'post') {
      await actOnCommunityPost(pending.value.id, payload)
    } else {
      await actOnCommunityComment(pending.value.id, payload)
    }
    reasonDialog.value?.close()
    ElMessage.success('内容状态已更新')
    await load()
  } catch (error) {
    reasonDialog.value?.close()
    showApiError(error)
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="社区治理" description="基础内容查询与隐藏、恢复、删除操作" />
    <section class="panel">
      <el-tabs v-model="activeTab" @tab-change="switchTab">
        <el-tab-pane label="动态" name="posts" />
        <el-tab-pane label="评论" name="comments" />
      </el-tabs>
      <div class="filter-bar">
        <el-input v-model="query.keyword" clearable placeholder="搜索正文" @keyup.enter="search" />
        <el-input v-model="query.userId" clearable placeholder="用户 ID" @keyup.enter="search" />
        <el-select v-model="query.status" clearable placeholder="内容状态">
          <el-option label="正常" :value="HitPostStatus.PUBLISHED" />
          <el-option label="隐藏" :value="HitPostStatus.HIDDEN" />
          <el-option label="删除" :value="HitPostStatus.DELETED" />
        </el-select>
        <el-button type="primary" plain @click="search">查询</el-button>
      </div>

      <el-table v-if="activeTab === 'posts'" v-loading="loading" :data="posts">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column label="作者" width="160">
          <template #default="{ row }"><strong>{{ row.displayName }}</strong><br /><small>#{{ row.userId }}</small></template>
        </el-table-column>
        <el-table-column prop="content" label="动态正文" min-width="380" show-overflow-tooltip />
        <el-table-column label="互动" width="180">
          <template #default="{ row }">{{ row.likeCount }} 赞 · {{ row.commentCount }} 评论</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :value="communityStatusNames[row.status]" /></template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-dropdown trigger="click">
              <el-button link type="primary">治理操作</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status !== HitPostStatus.HIDDEN" @click="openAction('post', row, CommunityContentAction.HIDE, '隐藏动态')">隐藏</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === HitPostStatus.HIDDEN" @click="openAction('post', row, CommunityContentAction.RESTORE, '恢复动态')">恢复</el-dropdown-item>
                  <el-dropdown-item divided @click="openAction('post', row, CommunityContentAction.DELETE, '删除动态')">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else v-loading="loading" :data="comments">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="postId" label="动态 ID" width="100" />
        <el-table-column label="作者" width="160">
          <template #default="{ row }"><strong>{{ row.displayName }}</strong><br /><small>#{{ row.userId }}</small></template>
        </el-table-column>
        <el-table-column prop="content" label="评论正文" min-width="420" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :value="communityStatusNames[row.status]" /></template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-dropdown trigger="click">
              <el-button link type="primary">治理操作</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status !== HitPostStatus.HIDDEN" @click="openAction('comment', row, CommunityContentAction.HIDE, '隐藏评论')">隐藏</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === HitPostStatus.HIDDEN" @click="openAction('comment', row, CommunityContentAction.RESTORE, '恢复评论')">恢复</el-dropdown-item>
                  <el-dropdown-item divided @click="openAction('comment', row, CommunityContentAction.DELETE, '删除评论')">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </section>
    <ReasonDialog ref="reasonDialog" @confirm="confirmAction" />
  </div>
</template>

<style scoped>
.filter-bar {
  margin-bottom: 18px;
}

small {
  color: var(--text-secondary);
}
</style>
