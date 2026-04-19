<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { inviteApi } from '@/api/invite'
import type {
  InviteOverviewVO,
  InviteReceiverAccountVO,
  InviteRecordVO,
  InviteRewardRecordVO,
  InviteRulesVO,
  InviteSettlementVO,
} from '@/api/types'
import { useToastStore } from '@/stores/toast'

const router = useRouter()
const toast = useToastStore()

const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const activeTab = ref<'records' | 'rewards' | 'settlements'>('records')

const overview = ref<InviteOverviewVO | null>(null)
const rules = ref<InviteRulesVO | null>(null)
const inviteRecords = ref<InviteRecordVO[]>([])
const rewardRecords = ref<InviteRewardRecordVO[]>([])
const settlementRecords = ref<InviteSettlementVO[]>([])
const receiverAccount = ref<InviteReceiverAccountVO | null>(null)

const receiverName = ref('')
const wechatNo = ref('')
const phone = ref('')
const remark = ref('')

const inviteCodeRaw = computed(() => String(overview.value?.myInviteCode || '').trim())
const codeText = computed(() => inviteCodeRaw.value || '邀请码生成中')
const settlementDayText = computed(() => rules.value?.settlementDay ?? overview.value?.settlementDay ?? 10)
const inviteLink = computed(() => {
  if (!inviteCodeRaw.value) return ''
  // 企业规范：邀请链接默认落到学生注册页，并自动透传邀请码，保证分享后最短转化路径。
  const path = `/auth/student?inviteCode=${encodeURIComponent(inviteCodeRaw.value)}`
  return typeof window === 'undefined' ? path : new URL(path, window.location.origin).toString()
})
const inviteHintText = computed(() => {
  const teacherRate = formatRate(rules.value?.teacherRewardRate ?? overview.value?.teacherRewardRate)
  const studentRate = formatRate(rules.value?.studentRewardRate ?? overview.value?.studentRewardRate)
  return `邀请教师成单返利 ${teacherRate}，邀请学生有效支付返利 ${studentRate}，每月 ${settlementDayText.value} 号统一结算。`
})
const summaryCards = computed(() => [
  { key: 'total', label: '邀请人数', value: String(overview.value?.totalInviteCount ?? 0), tone: 'sky' },
  { key: 'effective', label: '有效邀请', value: String(overview.value?.effectiveInviteCount ?? 0), tone: 'amber' },
  { key: 'reward', label: '累计返利', value: `¥${formatFen(overview.value?.totalRewardAmountFen)}`, tone: 'mint' },
  { key: 'pending', label: '待结算', value: `¥${formatFen(overview.value?.pendingSettlementAmountFen)}`, tone: 'rose' },
])

function formatFen(amountFen: number | null | undefined) {
  const amount = Number(amountFen || 0)
  return (amount / 100).toFixed(2)
}

function formatRate(rate: number | null | undefined) {
  const value = Number(rate || 0)
  return `${(value * 100).toFixed(value * 100 % 1 === 0 ? 0 : 2)}%`
}

function statusText(value: string | null | undefined) {
  const map: Record<string, string> = {
    ACTIVE: '已绑定',
    REGISTERED: '已注册',
    EFFECTIVE: '已转化',
    SETTLEABLE: '待结算',
    SETTLEMENT_PENDING: '待打款',
    PAID: '已打款',
    PENDING: '待结算',
    FROZEN: '冻结',
    INVALID: '失效',
    FAILED: '失败',
    REVERSED: '已冲正',
    CREATED: '已生成',
    PAYING: '打款中',
    CANCELED: '已取消',
  }
  return map[String(value || '')] || String(value || '--')
}

function userTypeText(value: number | null | undefined) {
  if (value === 1) return '教师'
  if (value === 2) return '学生'
  if (value === 3) return '机构'
  return '用户'
}

function rewardSceneText(value: string) {
  return value === 'INVITED_TUTOR_DEAL' ? '邀请教师成单' : '邀请学生支付'
}

function syncReceiverForm(account: InviteReceiverAccountVO | null) {
  receiverName.value = account?.receiverName || ''
  wechatNo.value = account?.wechatNo || ''
  phone.value = account?.phone || ''
  remark.value = account?.remark || ''
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [overviewRes, rulesRes, recordsRes, rewardsRes, settlementsRes, receiverRes] = await Promise.all([
      inviteApi.overview(),
      inviteApi.rules(),
      inviteApi.records({ pageSize: 5 }),
      inviteApi.rewards({ pageSize: 5 }),
      inviteApi.settlements({ pageSize: 5 }),
      inviteApi.receiverAccount(),
    ])
    overview.value = overviewRes
    rules.value = rulesRes
    inviteRecords.value = recordsRes.list || []
    rewardRecords.value = rewardsRes.list || []
    settlementRecords.value = settlementsRes.list || []
    receiverAccount.value = receiverRes
    syncReceiverForm(receiverRes)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function copyText(text: string, successText: string) {
  const value = String(text || '').trim()
  if (!value || value === '邀请码生成中') {
    toast.show('暂无可复制内容', 'info')
    return
  }
  try {
    await navigator.clipboard.writeText(value)
    toast.show(successText, 'success')
  } catch {
    toast.show('复制失败，请手动复制', 'error')
  }
}

function inviteMessage() {
  // 企业规范：分享文案保持轻量直给，优先突出“填写邀请码”和“有效订单返利”两个核心动作。
  return `注册时填写我的邀请码 ${inviteCodeRaw.value}，完成有效订单后我可获得平台返利，欢迎来体验。`
}

async function onSaveReceiverAccount() {
  if (saving.value) return
  saving.value = true
  try {
    const res = await inviteApi.saveReceiverAccount({
      receiverName: receiverName.value.trim(),
      wechatNo: wechatNo.value.trim(),
      phone: phone.value.trim(),
      remark: remark.value.trim() || undefined,
    })
    receiverAccount.value = res
    syncReceiverForm(res)
    toast.show('收款信息已保存', 'success')
  } catch (e) {
    toast.show(e instanceof Error ? e.message : '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="head">
        <button class="btn-back" type="button" @click="goBack">返回</button>
        <div class="title">邀请有礼</div>
        <div class="placeholder"></div>
      </div>

      <div v-if="error" class="hint error">{{ error }}</div>
      <div v-else-if="loading" class="hint">加载中...</div>

      <template v-else>
        <section class="promo card">
          <div class="promo-copy">
            <div class="promo-eyebrow">推广中心</div>
            <div class="promo-title">分享邀请码，邀请好友注册并产生有效订单，持续获取返利奖励</div>
            <div class="promo-sub">平台基于真实信息费订单生成返利明细，并在每月 {{ settlementDayText }} 号统一结算到你配置的微信收款账户。</div>
          </div>
          <div class="promo-badge">规则实时生效</div>
        </section>

        <section class="invite-panel card">
          <div class="section-head compact">
            <div class="section-title">我的邀请码</div>
            <div class="section-sub">分享邀请码或邀请链接，对方注册时填写后即可建立邀请关系</div>
          </div>

          <div class="share-grid">
            <div class="share-main">
              <div class="share-block">
                <div class="share-label">邀请码</div>
                <button class="share-row" type="button" @click="copyText(inviteCodeRaw, '邀请码已复制')">
                  <span class="share-value share-code">{{ codeText }}</span>
                  <span class="share-action">复制</span>
                </button>
              </div>

              <div class="share-block">
                <div class="share-label">邀请链接</div>
                <button class="share-row" type="button" @click="copyText(inviteLink, '邀请链接已复制')">
                  <span class="share-value share-link">{{ inviteLink || '邀请码生成后将自动展示邀请链接' }}</span>
                  <span class="share-action">复制</span>
                </button>
              </div>

              <div class="share-tip">
                <span class="share-tip-icon">!</span>
                <span>{{ inviteHintText }}</span>
              </div>
            </div>

            <div class="share-side">
              <button class="btn btn-primary btn-block" type="button" @click="copyText(inviteLink, '邀请链接已复制')">复制邀请链接</button>
              <button class="btn btn-ghost btn-block" type="button" @click="copyText(inviteMessage(), '邀请文案已复制')">复制邀请文案</button>
              <div class="share-side-note">邀请链接会自动带上邀请码，好友打开后注册页会自动预填，转化路径更短。</div>
            </div>
          </div>
        </section>

        <section class="stats-grid">
          <article v-for="item in summaryCards" :key="item.key" class="stat-card card" :class="`tone-${item.tone}`">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value">{{ item.value }}</div>
          </article>
        </section>

        <section class="content-grid">
          <div class="main-stack">
            <section class="card">
              <div class="tabbar">
                <button class="tab-pill" :class="{ active: activeTab === 'records' }" type="button" @click="activeTab = 'records'">邀请记录</button>
                <button class="tab-pill" :class="{ active: activeTab === 'rewards' }" type="button" @click="activeTab = 'rewards'">返利明细</button>
                <button class="tab-pill" :class="{ active: activeTab === 'settlements' }" type="button" @click="activeTab = 'settlements'">结算记录</button>
              </div>

              <template v-if="activeTab === 'records'">
                <div class="section-head compact">
                  <div class="section-title">邀请记录</div>
                  <div class="section-sub">展示最近邀请对象、注册时间与转化进展</div>
                </div>
                <div v-if="inviteRecords.length <= 0" class="empty">暂无邀请记录</div>
                <div v-else class="list">
                  <div v-for="item in inviteRecords" :key="item.inviteeUid" class="list-row">
                    <div class="list-main">
                      <div class="list-title">{{ item.inviteeDisplayName }}</div>
                      <div class="list-sub">{{ item.inviteePhoneMasked || '未留手机号' }} · {{ userTypeText(item.inviteeUserType) }} · {{ item.registeredAt }}</div>
                    </div>
                    <div class="list-side">
                      <div class="tag">{{ statusText(item.status) }}</div>
                      <div class="mini-note">{{ item.hasReward ? '已产生返利' : '暂未返利' }}</div>
                    </div>
                  </div>
                </div>
              </template>

              <template v-else-if="activeTab === 'rewards'">
                <div class="section-head compact">
                  <div class="section-title">返利明细</div>
                  <div class="section-sub">展示单笔返利来源、金额、比例与状态</div>
                </div>
                <div v-if="rewardRecords.length <= 0" class="empty">暂无返利明细</div>
                <div v-else class="list">
                  <div v-for="item in rewardRecords" :key="item.id" class="list-row">
                    <div class="list-main">
                      <div class="list-title">{{ item.inviteeDisplayName }} · ¥{{ formatFen(item.rewardAmountFen) }}</div>
                      <div class="list-sub">
                        {{ rewardSceneText(item.rewardScene) }}
                        · 基数 ¥{{ formatFen(item.baseAmountFen) }}
                        · 比例 {{ formatRate(item.rewardRate) }}
                        · {{ item.createdAt }}
                      </div>
                    </div>
                    <div class="list-side">
                      <div class="tag">{{ statusText(item.status) }}</div>
                    </div>
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="section-head compact">
                  <div class="section-title">结算记录</div>
                  <div class="section-sub">展示按月汇总的金额、打款时间与失败原因</div>
                </div>
                <div v-if="settlementRecords.length <= 0" class="empty">暂无结算记录</div>
                <div v-else class="list">
                  <div v-for="item in settlementRecords" :key="item.id" class="list-row">
                    <div class="list-main">
                      <div class="list-title">{{ item.settlementMonth }} · ¥{{ formatFen(item.totalAmountFen) }}</div>
                      <div class="list-sub">{{ item.payTime || '待打款' }}{{ item.failReason ? ` · ${item.failReason}` : '' }}</div>
                    </div>
                    <div class="list-side">
                      <div class="tag">{{ statusText(item.status) }}</div>
                    </div>
                  </div>
                </div>
              </template>
            </section>

            <section class="card">
              <div class="section-head compact">
                <div class="section-title">返利规则</div>
                <div class="section-sub">规则与比例以平台配置为准</div>
              </div>
              <div class="rule-list">
                <div class="rule-item">邀请教师成单返利：{{ formatRate(rules?.teacherRewardRate ?? overview?.teacherRewardRate) }}</div>
                <div class="rule-item">邀请学生有效支付返利：{{ formatRate(rules?.studentRewardRate ?? overview?.studentRewardRate) }}</div>
                <div class="rule-item">结算日期：每月 {{ settlementDayText }} 号</div>
                <div v-for="rule in rules?.ruleTextList || []" :key="rule" class="rule-item">{{ rule }}</div>
              </div>
            </section>
          </div>

          <aside class="side-stack">
            <section class="card">
              <div class="section-head compact">
                <div class="section-title">微信收款信息</div>
                <div class="section-sub">{{ rules?.receiverHint || '请确保收款信息准确，便于平台统一打款' }}</div>
              </div>
              <div class="receiver-status" :class="{ ready: receiverAccount?.configured }">
                {{ receiverAccount?.configured ? '已配置收款信息' : '尚未配置收款信息' }}
              </div>
              <div class="form-grid">
                <label class="field">
                  <div class="label">收款人姓名</div>
                  <input v-model="receiverName" class="input" placeholder="请输入收款人姓名" />
                </label>
                <label class="field">
                  <div class="label">微信号</div>
                  <input v-model="wechatNo" class="input" placeholder="请输入微信号" />
                </label>
                <label class="field">
                  <div class="label">手机号</div>
                  <input v-model="phone" class="input" placeholder="请输入手机号" />
                </label>
                <label class="field">
                  <div class="label">备注</div>
                  <input v-model="remark" class="input" placeholder="选填，便于财务核对" />
                </label>
              </div>
              <div class="actions">
                <button class="btn btn-primary btn-block" type="button" :disabled="saving" @click="onSaveReceiverAccount">
                  {{ saving ? '保存中...' : '保存收款信息' }}
                </button>
              </div>
            </section>
          </aside>
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(720px 320px at 8% 0%, rgba(0, 190, 189, 0.16), transparent 60%),
    radial-gradient(860px 360px at 92% 0%, rgba(30, 64, 175, 0.12), transparent 58%),
    linear-gradient(180deg, #f6f8fc 0%, #f8fbfb 100%);
  padding: 80px 16px 28px;
}

.container {
  width: min(1180px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.head {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 60px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
  display: grid;
  grid-template-columns: 72px 1fr 72px;
  align-items: center;
  padding: 0 16px;
  z-index: 10;
}

.btn-back {
  border: none;
  background: transparent;
  color: var(--text);
  cursor: pointer;
  font-weight: 700;
  text-align: left;
  padding: 0;
}

.title {
  text-align: center;
  font-size: 18px;
  font-weight: 900;
}

.placeholder {
  height: 1px;
}

.card {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 24px;
  padding: 22px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.promo {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
  background:
    radial-gradient(circle at top right, rgba(0, 190, 189, 0.2), transparent 34%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(241, 252, 251, 0.96));
  border-color: rgba(0, 190, 189, 0.16);
}

.promo-copy {
  display: grid;
  gap: 10px;
}

.promo-eyebrow {
  color: #0f766e;
  font-size: 14px;
  font-weight: 800;
}

.promo-title {
  max-width: 760px;
  font-size: 34px;
  line-height: 1.2;
  font-weight: 900;
  color: #0f172a;
}

.promo-sub {
  max-width: 760px;
  color: #475569;
  font-size: 15px;
  line-height: 1.7;
}

.promo-badge {
  flex-shrink: 0;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(0, 190, 189, 0.12);
  color: #0f766e;
  font-size: 13px;
  font-weight: 800;
}

.invite-panel {
  display: grid;
  gap: 16px;
}

.section-head {
  display: grid;
  gap: 6px;
  margin-bottom: 14px;
}

.section-head.compact {
  margin-bottom: 12px;
}

.section-title {
  font-size: 18px;
  font-weight: 900;
}

.section-sub {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.share-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(240px, 0.7fr);
  gap: 18px;
}

.share-main,
.share-side,
.main-stack,
.side-stack {
  display: grid;
  gap: 14px;
}

.share-block {
  display: grid;
  gap: 8px;
}

.share-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.share-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 14px;
  border: none;
  border-radius: 18px;
  background: rgba(241, 245, 249, 0.84);
  padding: 18px 20px;
  text-align: left;
  cursor: pointer;
}

.share-row:hover {
  background: rgba(226, 232, 240, 0.72);
}

.share-value {
  flex: 1;
  min-width: 0;
  color: #0f172a;
}

.share-code {
  font-size: 34px;
  line-height: 1;
  letter-spacing: 2px;
  font-weight: 900;
}

.share-link {
  font-size: 15px;
  line-height: 1.7;
  word-break: break-all;
}

.share-action {
  flex-shrink: 0;
  color: #0f766e;
  font-size: 13px;
  font-weight: 800;
}

.share-tip {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(240, 253, 250, 0.9);
  color: #0f766e;
  font-size: 14px;
  line-height: 1.7;
}

.share-tip-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 190, 189, 0.14);
  color: #0f766e;
  font-style: normal;
  font-weight: 900;
}

.share-side-note {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.7;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  display: grid;
  gap: 10px;
  min-height: 136px;
  align-content: center;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.92));
}

.stat-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.stat-value {
  color: #241a14;
  font-size: 32px;
  line-height: 1.1;
  font-weight: 900;
}

.tone-sky {
  border-color: rgba(83, 148, 255, 0.16);
}

.tone-amber {
  border-color: rgba(232, 165, 68, 0.18);
}

.tone-mint {
  border-color: rgba(56, 190, 111, 0.18);
}

.tone-rose {
  border-color: rgba(228, 108, 120, 0.18);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 0.8fr);
  gap: 18px;
}

.tabbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.tab-pill {
  border: none;
  border-radius: 999px;
  background: rgba(0, 190, 189, 0.08);
  color: #0f766e;
  padding: 11px 18px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}

.tab-pill.active {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 10px 20px rgba(0, 190, 189, 0.18);
}

.rule-list {
  display: grid;
  gap: 10px;
}

.rule-item {
  padding: 13px 14px;
  border-radius: 14px;
  background: rgba(240, 253, 250, 0.9);
  font-size: 14px;
  line-height: 1.7;
}

.receiver-status {
  margin-bottom: 14px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.06);
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.receiver-status.ready {
  background: rgba(230, 248, 234, 0.88);
  color: #1b7d4b;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.field {
  display: grid;
  gap: 8px;
}

.label {
  color: var(--muted);
  font-size: 12px;
}

.input {
  height: 44px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: #fff;
  padding: 0 12px;
  outline: none;
}

.input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-weak);
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.btn-block {
  width: 100%;
  justify-content: center;
}

.btn-ghost {
  background: rgba(0, 190, 189, 0.08);
  color: #0f766e;
}

.list {
  display: grid;
  gap: 10px;
}

.list-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.96);
}

.list-main {
  min-width: 0;
  flex: 1;
}

.list-side {
  display: grid;
  gap: 8px;
  justify-items: end;
}

.list-title {
  font-weight: 800;
}

.list-sub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.6;
}

.tag {
  flex-shrink: 0;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(0, 190, 189, 0.12);
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
}

.mini-note {
  color: var(--muted);
  font-size: 12px;
}

.hint {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(18, 42, 66, 0.06);
  font-size: 13px;
}

.hint.error {
  border: 1px solid rgba(255, 77, 79, 0.28);
  background: rgba(255, 77, 79, 0.08);
}

.empty {
  color: var(--muted);
  font-size: 13px;
}

@media (max-width: 1080px) {
  .share-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .promo {
    display: grid;
  }

  .promo-title {
    font-size: 28px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .share-code {
    font-size: 28px;
  }

  .share-row {
    padding: 16px;
  }

  .list-row {
    display: grid;
  }

  .list-side {
    justify-items: start;
  }
}
</style>
