<template>
  <div class="card box">
    <div class="head">
      <div class="left">
        <div class="title">付款记录</div>
        <div class="sub">支持按订单号/业务关联/渠道/状态/时间筛选</div>
      </div>
      <div class="right">
        <button class="btn" type="button" :disabled="loading" @click="load">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
    </div>

    <div class="filters">
      <input v-model.trim="qOrderNo" class="input" placeholder="订单号 orderNo" />
      <input v-model.trim="qUserId" class="input" placeholder="用户ID userId" />
      <input v-model.trim="qContextType" class="input" placeholder="contextType" />
      <input v-model.trim="qContextId" class="input" placeholder="contextId" />
      <select v-model="qChannel" class="input">
        <option value="">全部渠道</option>
        <option value="WECHAT">微信</option>
        <option value="ALIPAY">支付宝</option>
      </select>
      <select v-model="qStatus" class="input">
        <option value="">全部状态</option>
        <option value="PENDING">待支付</option>
        <option value="SUCCESS">成功</option>
        <option value="FAILED">失败</option>
        <option value="CLOSED">关闭/过期</option>
      </select>
      <input v-model.trim="qStartTime" class="input" placeholder="开始时间 yyyy-MM-dd HH:mm:ss" />
      <input v-model.trim="qEndTime" class="input" placeholder="结束时间 yyyy-MM-dd HH:mm:ss" />
      <button class="btn btn-muted" type="button" :disabled="loading" @click="applyFilters">查询</button>
    </div>

    <div v-if="errorText" class="error">{{ errorText }}</div>

    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th style="width: 90px">ID</th>
            <th style="width: 220px">订单号</th>
            <th style="width: 110px">用户</th>
            <th style="width: 110px">金额</th>
            <th style="width: 110px">渠道</th>
            <th style="width: 110px">状态</th>
            <th style="width: 190px">创建时间</th>
            <th style="width: 190px">支付时间</th>
            <th>业务关联</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>{{ row.id }}</td>
            <td>
              <RouterLink class="link" :to="{ name: 'paymentOrderDetail', params: { orderNo: row.orderNo } }">
                {{ row.orderNo }}
              </RouterLink>
            </td>
            <td>{{ row.userId }}</td>
            <td>
              <span class="badge">¥{{ (row.amount / 100).toFixed(2) }}</span>
            </td>
            <td>{{ channelText(row.channel) }}</td>
            <td>
              <span class="badge" :class="statusClass(row.status)">{{ statusText(row.status) }}</span>
            </td>
            <td>{{ timeText(row.createTime) }}</td>
            <td>{{ timeText(row.successTime) }}</td>
            <td>{{ row.contextType }} · {{ row.contextId }}</td>
          </tr>
          <tr v-if="rows.length === 0 && !loading">
            <td colspan="9">
              <div class="empty">暂无记录</div>
            </td>
          </tr>
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

import { listPaymentOrders, type PaymentChannel, type PaymentOrderRecord, type PaymentStatus } from '@/api/paymentOrders'

const rows = ref<PaymentOrderRecord[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const errorText = ref<string | null>(null)

const qOrderNo = ref('')
const qUserId = ref('')
const qContextType = ref('')
const qContextId = ref('')
const qChannel = ref<string>('')
const qStatus = ref<string>('')
const qStartTime = ref('')
const qEndTime = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

function timeText(s?: string | null) {
  if (!s) return '-'
  return String(s).replace('T', ' ').slice(0, 19)
}

function statusText(s: PaymentStatus) {
  if (s === 'PENDING') return '待支付'
  if (s === 'SUCCESS') return '成功'
  if (s === 'FAILED') return '失败'
  if (s === 'CLOSED') return '关闭'
  return s
}

function statusClass(s: PaymentStatus) {
  if (s === 'SUCCESS') return 'ok'
  if (s === 'FAILED') return 'danger'
  if (s === 'CLOSED') return 'muted'
  return ''
}

function channelText(c: PaymentChannel) {
  if (c === 'WECHAT') return '微信'
  if (c === 'ALIPAY') return '支付宝'
  return c
}

function toNum(s: string): number | undefined {
  const n = Number(s)
  return Number.isFinite(n) ? n : undefined
}

async function load() {
  if (loading.value) return
  loading.value = true
  errorText.value = null
  try {
    const res = await listPaymentOrders({
      page: page.value,
      size: size.value,
      orderNo: qOrderNo.value || undefined,
      userId: toNum(qUserId.value),
      contextType: qContextType.value || undefined,
      contextId: toNum(qContextId.value),
      channel: (qChannel.value as PaymentChannel) || undefined,
      status: (qStatus.value as PaymentStatus) || undefined,
      startTime: qStartTime.value || undefined,
      endTime: qEndTime.value || undefined,
    })
    rows.value = res.records
    total.value = Number(res.total || 0)
  } catch (e) {
    errorText.value = e && typeof e === 'object' && 'message' in e ? String((e as { message?: unknown }).message) : '加载失败'
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  load()
}

watch([page, size], () => {
  if (page.value > totalPages.value) page.value = totalPages.value
  load()
})

onMounted(load)
</script>

<style scoped>
.box {
  padding: 14px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.title {
  font-weight: 800;
}

.sub {
  color: var(--muted);
  font-size: 12px;
  margin-top: 4px;
}

.filters {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  align-items: center;
}

.link {
  color: var(--primary);
  font-weight: 700;
}

.badge.ok {
  border-color: rgba(0, 168, 112, 0.3);
  background: rgba(0, 168, 112, 0.08);
  color: #00a870;
}

.badge.danger {
  border-color: rgba(212, 56, 13, 0.25);
  background: rgba(212, 56, 13, 0.06);
  color: #d4380d;
}

.badge.muted {
  color: var(--muted);
}

@media (max-width: 980px) {
  .filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

