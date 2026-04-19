<template>
  <div class="card box">
    <div class="head">
      <div>
        <div class="title">邀请返利</div>
        <div class="sub">查看邀请关系、返利明细与月度结算单，财务可在此标记打款结果</div>
      </div>
      <button class="btn" type="button" :disabled="loading" @click="load">{{ loading ? '刷新中...' : '刷新' }}</button>
    </div>

    <section class="system-card">
      <div class="system-copy">
        <div class="system-kicker">系统邀请码</div>
        <div class="system-title">{{ systemConfig.enabled ? '推广期权益已开启' : '推广期权益已关闭' }}</div>
        <div class="system-desc">
          教师使用系统邀请码注册后，信息费按 {{ rateText(systemConfig.tutorInfoFeeDiscountRate) }} 支付；学生使用系统邀请码注册后，可按教师实付信息费返现 {{ percentText(systemConfig.studentRewardRate) }}。
        </div>
      </div>
      <div class="system-form">
        <label class="check">
          <input v-model="systemConfig.enabled" type="checkbox" />
          <span>启用系统邀请码</span>
        </label>
        <input v-model.trim="systemConfig.systemInviteCode" class="input" placeholder="系统邀请码" />
        <input v-model.trim="systemConfig.systemInviteLink" class="input" placeholder="系统邀请链接" />
        <div class="two">
          <input v-model.number="systemConfig.tutorInfoFeeDiscountRate" class="input" type="number" min="0.01" max="1" step="0.01" placeholder="教师折扣" />
          <input v-model.number="systemConfig.studentRewardRate" class="input" type="number" min="0" max="1" step="0.01" placeholder="学生返现" />
        </div>
        <input v-model.trim="systemConfig.promoTitle" class="input" placeholder="推广标题" />
        <input v-model.trim="systemConfig.promoDesc" class="input" placeholder="推广说明" />
        <button class="btn btn-primary" type="button" :disabled="systemSaving" @click="saveSystemConfig">
          {{ systemSaving ? '保存中...' : '保存系统邀请码配置' }}
        </button>
      </div>
    </section>

    <div class="tabs">
      <button class="tab" :class="{ active: activeTab === 'relations' }" type="button" @click="switchTab('relations')">邀请关系</button>
      <button class="tab" :class="{ active: activeTab === 'rewards' }" type="button" @click="switchTab('rewards')">返利明细</button>
      <button class="tab" :class="{ active: activeTab === 'settlements' }" type="button" @click="switchTab('settlements')">结算单</button>
    </div>

    <div class="filters">
      <input v-model.trim="qInviterUid" class="input" placeholder="邀请人 UID" />
      <input v-model.trim="qInviteeUid" class="input" placeholder="被邀请人 UID" />
      <input v-if="activeTab === 'settlements'" v-model.trim="qUserId" class="input" placeholder="结算用户 UID" />
      <input v-if="activeTab !== 'relations'" v-model.trim="qSettlementMonth" class="input" placeholder="结算月份 yyyy-MM" />
      <select v-model="qStatus" class="input">
        <option value="">全部状态</option>
        <option v-for="item in statusOptions" :key="item" :value="item">{{ statusText(item) }}</option>
      </select>
      <select v-if="activeTab === 'rewards'" v-model="qScene" class="input">
        <option value="">全部场景</option>
        <option value="INVITED_TUTOR_DEAL">邀请教师成单</option>
        <option value="INVITED_STUDENT_PAID">邀请学生支付</option>
      </select>
      <button class="btn btn-muted" type="button" :disabled="loading" @click="applyFilters">查询</button>
    </div>

    <div v-if="errorText" class="error">{{ errorText }}</div>

    <div v-if="activeTab === 'relations'" class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>邀请人</th>
            <th>被邀请人</th>
            <th>邀请码</th>
            <th>状态</th>
            <th>绑定时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in relationRows" :key="row.id">
            <td>{{ row.id }}</td>
            <td>{{ userText(row.inviterUid, row.inviterName, row.inviterPhone) }}</td>
            <td>{{ userText(row.inviteeUid, row.inviteeName, row.inviteePhone) }} · {{ userTypeText(row.inviteeUserType) }}</td>
            <td><span class="badge">{{ row.inviteCode }}</span></td>
            <td><span class="badge">{{ statusText(row.status) }}</span></td>
            <td>{{ timeText(row.bindTime || row.createTime) }}</td>
          </tr>
          <tr v-if="relationRows.length === 0 && !loading"><td colspan="6"><div class="empty">暂无邀请关系</div></td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="activeTab === 'rewards'" class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>邀请人</th>
            <th>被邀请人</th>
            <th>场景</th>
            <th>基数</th>
            <th>比例</th>
            <th>返利</th>
            <th>状态</th>
            <th>创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rewardRows" :key="row.id">
            <td>{{ row.id }}</td>
            <td>{{ userText(row.inviterUid, row.inviterName, row.inviterPhone) }}</td>
            <td>{{ userText(row.inviteeUid, row.inviteeName, row.inviteePhone) }}</td>
            <td>{{ sceneText(row.rewardScene) }}</td>
            <td>¥{{ fenText(row.baseAmountFen) }}</td>
            <td>{{ Math.round(Number(row.rewardRate || 0) * 100) }}%</td>
            <td><span class="badge ok">¥{{ fenText(row.rewardAmountFen) }}</span></td>
            <td><span class="badge">{{ statusText(row.status) }}</span></td>
            <td>{{ timeText(row.createTime) }}</td>
          </tr>
          <tr v-if="rewardRows.length === 0 && !loading"><td colspan="9"><div class="empty">暂无返利明细</div></td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="activeTab === 'settlements'" class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户</th>
            <th>月份</th>
            <th>结算金额</th>
            <th>状态</th>
            <th>收款快照</th>
            <th>打款时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in settlementRows" :key="row.id">
            <td>{{ row.id }}</td>
            <td>{{ userText(row.userId, row.userName, row.userPhone) }}</td>
            <td>{{ row.settlementMonth }}</td>
            <td><span class="badge ok">¥{{ fenText(row.totalAmountFen) }}</span></td>
            <td><span class="badge" :class="settlementStatusClass(row.status)">{{ statusText(row.status) }}</span></td>
            <td class="snapshot">{{ row.receiverSnapshotJson || '-' }}</td>
            <td>{{ timeText(row.payTime) }}</td>
            <td>
              <div class="actions">
                <button class="btn btn-primary" type="button" :disabled="loading || row.status === 'PAID'" @click="markPaid(row.id)">已打款</button>
                <button class="btn btn-danger" type="button" :disabled="loading || row.status === 'PAID'" @click="markFailed(row.id)">失败</button>
              </div>
            </td>
          </tr>
          <tr v-if="settlementRows.length === 0 && !loading"><td colspan="8"><div class="empty">暂无结算单</div></td></tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
      <button class="btn btn-muted" type="button" :disabled="page <= 1 || loading" @click="page -= 1">上一页</button>
      <div class="pager-text">第 {{ page }} 页 / 共 {{ totalPages }} 页（{{ total }} 条）</div>
      <button class="btn btn-muted" type="button" :disabled="page >= totalPages || loading" @click="page += 1">下一页</button>
      <select v-model.number="size" class="input size">
        <option :value="10">10 / 页</option>
        <option :value="20">20 / 页</option>
        <option :value="50">50 / 页</option>
      </select>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import {
  getInviteSystemConfig,
  listInviteRelations,
  listInviteRewards,
  listInviteSettlements,
  markInviteSettlementFailed,
  markInviteSettlementPaid,
  saveInviteSystemConfig as saveInviteSystemConfigApi,
} from '@/api/invite'
import type { AdminInviteRelation, AdminInviteReward, AdminInviteSettlement, AdminInviteSystemConfig } from '@/api/types'

type TabKey = 'relations' | 'rewards' | 'settlements'

const activeTab = ref<TabKey>('relations')
const relationRows = ref<AdminInviteRelation[]>([])
const rewardRows = ref<AdminInviteReward[]>([])
const settlementRows = ref<AdminInviteSettlement[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const errorText = ref<string | null>(null)
const systemSaving = ref(false)
const systemConfig = ref<AdminInviteSystemConfig>({
  enabled: true,
  systemInviteCode: 'CHUANGZHI',
  systemInviteLink: '',
  tutorInfoFeeDiscountRate: 0.5,
  studentRewardRate: 0.13,
  promoTitle: '创智推广专属福利',
  promoDesc: '使用创智推广码注册后，教师信息费享受推广期减半，学生可按教师实付信息费获得返现。',
})

const qInviterUid = ref('')
const qInviteeUid = ref('')
const qUserId = ref('')
const qStatus = ref('')
const qScene = ref('')
const qSettlementMonth = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))
const statusOptions = computed(() => {
  if (activeTab.value === 'settlements') return ['CREATED', 'PAYING', 'PAID', 'FAILED', 'CANCELED']
  if (activeTab.value === 'rewards') return ['PENDING', 'SETTLEMENT_PENDING', 'PAID', 'FROZEN', 'FAILED', 'REVERSED']
  return ['ACTIVE', 'FROZEN', 'INVALID']
})

function toNum(value: string): number | undefined {
  const n = Number(value)
  return Number.isFinite(n) && value.trim() !== '' ? n : undefined
}

function timeText(value?: string | null) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function fenText(value?: number | null) {
  return (Number(value || 0) / 100).toFixed(2)
}

function percentText(value?: number | null) {
  return `${Math.round(Number(value || 0) * 100)}%`
}

function rateText(value?: number | null) {
  const n = Number(value || 0)
  return n > 0 ? `${Math.round(n * 100)}%` : '-'
}

function userText(uid?: number | null, name?: string | null, phone?: string | null) {
  return `${name || phone || '用户'}（${uid ?? '-'}）`
}

function userTypeText(value?: number | null) {
  if (value === 1) return '教师'
  if (value === 2) return '学生'
  if (value === 3) return '机构'
  return '未知'
}

function sceneText(value: string) {
  if (value === 'INVITED_TUTOR_DEAL') return '邀请教师成单'
  if (value === 'INVITED_STUDENT_PAID') return '邀请学生支付'
  return value
}

function statusText(value: string) {
  const map: Record<string, string> = {
    ACTIVE: '有效',
    FROZEN: '冻结',
    INVALID: '失效',
    PENDING: '待结算',
    SETTLEMENT_PENDING: '待打款',
    PAID: '已打款',
    CREATED: '已生成',
    PAYING: '打款中',
    FAILED: '失败',
    CANCELED: '取消',
    REVERSED: '已冲正',
  }
  return map[value] || value
}

function settlementStatusClass(value: string) {
  if (value === 'PAID') return 'ok'
  if (value === 'FAILED') return 'danger'
  return ''
}

function switchTab(tab: TabKey) {
  // 企业规范：切换标签时重置分页与当前列表，避免沿用上一标签的筛选结果造成数据误读。
  activeTab.value = tab
  page.value = 1
  total.value = 0
  relationRows.value = []
  rewardRows.value = []
  settlementRows.value = []
  load()
}

async function load() {
  if (loading.value) return
  loading.value = true
  errorText.value = null
  try {
    if (activeTab.value === 'relations') {
      const res = await listInviteRelations({ page: page.value, size: size.value, inviterUid: toNum(qInviterUid.value), inviteeUid: toNum(qInviteeUid.value), status: qStatus.value || undefined })
      relationRows.value = res.records
      total.value = Number(res.total || 0)
    } else if (activeTab.value === 'rewards') {
      const res = await listInviteRewards({
        page: page.value,
        size: size.value,
        inviterUid: toNum(qInviterUid.value),
        inviteeUid: toNum(qInviteeUid.value),
        status: qStatus.value || undefined,
        scene: qScene.value || undefined,
        settlementMonth: qSettlementMonth.value || undefined,
      })
      rewardRows.value = res.records
      total.value = Number(res.total || 0)
    } else {
      const res = await listInviteSettlements({ page: page.value, size: size.value, userId: toNum(qUserId.value), status: qStatus.value || undefined, settlementMonth: qSettlementMonth.value || undefined })
      settlementRows.value = res.records
      total.value = Number(res.total || 0)
    }
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadSystemConfig() {
  try {
    systemConfig.value = await getInviteSystemConfig()
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '系统邀请码配置加载失败'
  }
}

async function saveSystemConfig() {
  if (systemSaving.value) return
  systemSaving.value = true
  errorText.value = null
  try {
    // 企业规范：系统邀请码统一大写保存，避免注册侧大小写差异造成推广权益丢失。
    systemConfig.value.systemInviteCode = systemConfig.value.systemInviteCode.trim().toUpperCase()
    systemConfig.value = await saveInviteSystemConfigApi(systemConfig.value)
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '系统邀请码配置保存失败'
  } finally {
    systemSaving.value = false
  }
}

function applyFilters() {
  // 查询条件变化后统一回到第一页，避免大页码在新条件下出现空页假象。
  page.value = 1
  load()
}

async function markPaid(id: number) {
  if (!window.confirm('确认该结算单已完成微信打款？')) return
  await markInviteSettlementPaid(id)
  await load()
}

async function markFailed(id: number) {
  const reason = window.prompt('请输入打款失败原因', '微信收款信息异常') || ''
  if (!reason.trim()) return
  await markInviteSettlementFailed(id, reason.trim())
  await load()
}

watch([page, size], () => {
  // 页码、分页大小变化后自动刷新列表，保持运营后台操作心智简单直接。
  if (page.value > totalPages.value) page.value = totalPages.value
  load()
})

onMounted(() => {
  load()
  loadSystemConfig()
})
</script>

<style scoped>
.box {
  padding: 14px;
}

.head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.title {
  font-weight: 800;
}

.sub {
  color: var(--muted);
  font-size: 12px;
  margin-top: 4px;
}

.tabs,
.actions,
.pager {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.tabs {
  margin-top: 12px;
}

.system-card {
  margin-top: 16px;
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(360px, 1.1fr);
  gap: 16px;
  padding: 18px;
  border-radius: 18px;
  border: 1px solid rgba(0, 190, 189, 0.16);
  background:
    radial-gradient(circle at top right, rgba(0, 190, 189, 0.14), transparent 34%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(242, 252, 251, 0.92));
}

.system-copy {
  display: grid;
  align-content: center;
  gap: 8px;
}

.system-kicker {
  color: #0f766e;
  font-size: 13px;
  font-weight: 900;
}

.system-title {
  color: #0f172a;
  font-size: 24px;
  font-weight: 900;
}

.system-desc {
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.system-form {
  display: grid;
  gap: 10px;
}

.two {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}

.tab {
  border: 1px solid var(--border);
  background: transparent;
  color: var(--muted);
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
}

.tab.active {
  background: rgba(0, 190, 189, 0.1);
  border-color: rgba(0, 190, 189, 0.35);
  color: var(--text);
}

.filters {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.table-wrap {
  margin-top: 12px;
  overflow-x: auto;
}

.empty,
.error {
  padding: 14px;
  color: var(--muted);
}

.error {
  color: var(--danger);
}

.snapshot {
  max-width: 280px;
  word-break: break-all;
  color: var(--muted);
  font-size: 12px;
}

.ok {
  color: #047857;
  border-color: rgba(4, 120, 87, 0.25);
  background: rgba(4, 120, 87, 0.08);
}

.danger {
  color: var(--danger);
  border-color: rgba(239, 68, 68, 0.25);
  background: rgba(239, 68, 68, 0.08);
}

.pager {
  margin-top: 12px;
}

.pager-text {
  color: var(--muted);
}

.size {
  width: 110px;
}

@media (max-width: 1100px) {
  .filters {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
