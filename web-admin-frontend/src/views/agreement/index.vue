<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-toolbar">
        <el-input v-model="query.name" placeholder="承租人姓名" clearable style="width: 150px" @keyup.enter="search" />
        <el-input v-model="query.phone" placeholder="手机号" clearable style="width: 150px" @keyup.enter="search" />
        <el-select v-model="query.apartmentId" placeholder="公寓" clearable filterable style="width: 200px">
          <el-option v-for="a in apartments" :key="a.id" :label="a.name" :value="a.id!" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div class="spacer"></div>
        <el-button type="primary" :icon="Plus" @click="openForm()">新增租约</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="承租人" width="100" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="125" />
        <el-table-column label="公寓 / 房间" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.apartmentInfo?.name || '-' }} / {{ row.roomInfo?.roomNumber || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="租期" width="200">
          <template #default="{ row }">
            {{ formatDate(row.leaseStartDate) }} ~ {{ formatDate(row.leaseEndDate) }}
          </template>
        </el-table-column>
        <el-table-column label="月租 / 押金" width="130" align="right">
          <template #default="{ row }">
            <div class="text-danger">¥{{ Number(row.rent || 0).toFixed(0) }}</div>
            <div class="text-muted">押 ¥{{ Number(row.deposit || 0).toFixed(0) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="dictType(LEASE_STATUS, row.status)" size="small">
              {{ dictLabel(LEASE_STATUS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.id!)">详情</el-button>
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button link type="primary" @click="changeStatus(row)">改状态</el-button>            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="租约详情" width="620px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="租约ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="dictType(LEASE_STATUS, detail.status)" size="small">
            {{ dictLabel(LEASE_STATUS, detail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="承租人">{{ detail.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号" :span="2">{{ detail.identificationNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公寓">{{ detail.apartmentInfo?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="房间">{{ detail.roomInfo?.roomNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="租期">
          {{ formatDate(detail.leaseStartDate) }} ~ {{ formatDate(detail.leaseEndDate) }}
        </el-descriptions-item>
        <el-descriptions-item label="租期类型">
          {{ detail.leaseTerm ? `${detail.leaseTerm.monthCount}${detail.leaseTerm.unit || '个月'}` : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ detail.paymentType?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ dictLabel(LEASE_SOURCE, detail.sourceType) }}</el-descriptions-item>
        <el-descriptions-item label="月租">¥{{ Number(detail.rent || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="押金">¥{{ Number(detail.deposit || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.additionalInfo || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 变更状态 -->
    <el-dialog v-model="statusDialogVisible" title="变更租约状态" width="420px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="租约">
          <span>{{ statusEditingName || '-' }}</span>
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="dictType(LEASE_STATUS, statusEditingCurrent)" size="small">
            {{ dictLabel(LEASE_STATUS, statusEditingCurrent) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="变更为">
          <el-select v-model="statusDraft" placeholder="请选择新状态" style="width: 100%">
            <el-option v-for="s in dictOptions(LEASE_STATUS)" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="statusSaving"
          :disabled="statusDraft === statusEditingCurrent"
          @click="submitStatus"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增 / 编辑 -->
    <el-drawer v-model="formVisible" :title="form.id ? '编辑租约' : '新增租约'" size="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="96px">
        <el-form-item label="公寓" prop="apartmentId">
          <el-select v-model="form.apartmentId" filterable placeholder="请选择公寓" style="width: 100%" @change="onApartmentChange">
            <el-option v-for="a in apartments" :key="a.id" :label="a.name" :value="a.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间" prop="roomId">
          <el-select v-model="form.roomId" filterable placeholder="请选择房间" style="width: 100%" :disabled="!form.apartmentId" @change="onRoomChange">
            <el-option v-for="r in rooms" :key="r.id" :label="r.roomNumber" :value="r.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="承租人" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.identificationNumber" maxlength="20" placeholder="选填" />
        </el-form-item>
        <el-form-item label="起租日期" prop="leaseStartDate">
          <el-date-picker
            v-model="form.leaseStartDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择开始日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="到期日期" prop="leaseEndDate">
          <el-date-picker
            v-model="form.leaseEndDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择结束日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="租期类型">
          <el-select v-model="form.leaseTermId" clearable placeholder="请选择租期" style="width: 100%">
            <el-option
              v-for="t in leaseTerms"
              :key="t.id"
              :label="`${t.monthCount}${t.unit || '个月'}`"
              :value="t.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="form.paymentTypeId" clearable placeholder="请选择支付方式" style="width: 100%">
            <el-option v-for="p in paymentTypes" :key="p.id" :label="p.name" :value="p.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="月租金" prop="rent">
          <el-input-number v-model="form.rent" :min="0" :step="100" :precision="2" style="width: 200px" />
        </el-form-item>
        <el-form-item label="押金">
          <el-input-number v-model="form.deposit" :min="0" :step="100" :precision="2" style="width: 200px" />
        </el-form-item>
        <el-form-item label="租约状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in dictOptions(LEASE_STATUS)" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.additionalInfo" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { agreementApi, apartmentApi, paymentApi, roomApi, termApi } from '@/api'
import { LEASE_SOURCE, LEASE_STATUS, dictLabel, dictOptions, dictType } from '@/utils/dict'
import type { AgreementVo, ApartmentItemVo, LeaseTerm, PaymentType, RoomInfo } from '@/types/api'

const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const rows = ref<AgreementVo[]>([])
const apartments = ref<ApartmentItemVo[]>([])

const detailVisible = ref(false)
const detail = ref<AgreementVo | null>(null)

const formVisible = ref(false)
const formRef = ref<FormInstance>()
const rooms = ref<RoomInfo[]>([])
const leaseTerms = ref<LeaseTerm[]>([])
const paymentTypes = ref<PaymentType[]>([])

const query = reactive({
  current: 1,
  size: 10,
  name: '',
  phone: '',
  apartmentId: undefined as number | undefined
})

const form = reactive<AgreementVo>({
  id: undefined,
  apartmentId: undefined,
  roomId: undefined,
  name: '',
  phone: '',
  identificationNumber: '',
  leaseStartDate: '',
  leaseEndDate: '',
  leaseTermId: undefined,
  paymentTypeId: undefined,
  rent: 0,
  deposit: 0,
  status: 1,
  sourceType: 1,
  additionalInfo: ''
})

const formRules: FormRules = {
  apartmentId: [{ required: true, message: '请选择公寓', trigger: 'change' }],
  roomId: [{ required: true, message: '请选择房间', trigger: 'change' }],
  name: [{ required: true, message: '请输入承租人姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  leaseStartDate: [{ required: true, message: '请选择起租日期', trigger: 'change' }],
  leaseEndDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }],
  rent: [{ required: true, message: '请填写月租金', trigger: 'blur' }]
}

onMounted(async () => {
  const [aptRes, payRes, termRes] = await Promise.allSettled([
    apartmentApi.page({ current: 1, size: 200 }),
    paymentApi.list(),
    termApi.list()
  ])
  if (aptRes.status === 'fulfilled') apartments.value = aptRes.value.data.data?.records || []
  if (payRes.status === 'fulfilled') paymentTypes.value = payRes.value.data.data || []
  if (termRes.status === 'fulfilled') leaseTerms.value = termRes.value.data.data || []
  await load()
})

async function load() {
  loading.value = true
  try {
    const res = await agreementApi.page({ ...query })
    const data = res.data.data
    rows.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

function reset() {
  query.current = 1
  query.name = ''
  query.phone = ''
  query.apartmentId = undefined
  load()
}

function formatDate(value?: string) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

async function showDetail(id: number) {
  try {
    const res = await agreementApi.detail(id)
    detail.value = res.data.data
    detailVisible.value = true
  } catch {
    /* 拦截器已提示 */
  }
}

function openForm(row?: AgreementVo) {
  formVisible.value = true
  rooms.value = []
  Object.assign(form, {
    id: undefined,
    apartmentId: undefined,
    roomId: undefined,
    name: '',
    phone: '',
    identificationNumber: '',
    leaseStartDate: '',
    leaseEndDate: '',
    leaseTermId: undefined,
    paymentTypeId: undefined,
    rent: 0,
    deposit: 0,
    status: 1,
    sourceType: 1,
    additionalInfo: ''
  })
  if (row) {
    Object.assign(form, {
      id: row.id,
      apartmentId: row.apartmentId,
      roomId: row.roomId,
      name: row.name,
      phone: row.phone,
      identificationNumber: row.identificationNumber,
      leaseStartDate: formatDateOnly(row.leaseStartDate),
      leaseEndDate: formatDateOnly(row.leaseEndDate),
      leaseTermId: row.leaseTermId,
      paymentTypeId: row.paymentTypeId,
      rent: Number(row.rent || 0),
      deposit: Number(row.deposit || 0),
      status: Number(row.status ?? 1),
      sourceType: Number(row.sourceType ?? 1),
      additionalInfo: row.additionalInfo
    })
    if (form.apartmentId) {
      roomApi
        .listBasicByApartment(form.apartmentId)
        .then(res => {
          rooms.value = res.data.data || []
        })
        .catch(() => {})
    }
  }
  formRef.value?.clearValidate()
}

function formatDateOnly(value?: string) {
  if (!value) return ''
  return String(value).slice(0, 10)
}

async function onApartmentChange() {
  form.roomId = undefined
  rooms.value = []
  if (!form.apartmentId) return
  try {
    const res = await roomApi.listBasicByApartment(form.apartmentId)
    rooms.value = res.data.data || []
  } catch {
    /* ignore */
  }
}

function onRoomChange() {
  const room = rooms.value.find(r => r.id === form.roomId)
  if (room && !form.rent) {
    form.rent = Number(room.rent || 0)
  }
  if (room && !form.deposit) {
    form.deposit = Number(room.rent || 0)
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.leaseStartDate && form.leaseEndDate && form.leaseEndDate <= form.leaseStartDate) {
    ElMessage.warning('到期日期必须晚于起租日期')
    return
  }

  saving.value = true
  try {
    await agreementApi.save({ ...form })
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

/* ------------------------------------------------------------ 变更状态 */
const statusDialogVisible = ref(false)
const statusSaving = ref(false)
const statusEditingId = ref<number>()
const statusEditingName = ref('')
const statusEditingCurrent = ref<number>(0)
const statusDraft = ref<number>()

function changeStatus(row: AgreementVo) {
  statusEditingId.value = row.id
  statusEditingName.value = `${row.name || '-'} / ${row.apartmentInfo?.name || '-'} ${row.roomInfo?.roomNumber || ''}`
  statusEditingCurrent.value = Number(row.status ?? 0)
  statusDraft.value = Number(row.status ?? 0)
  statusDialogVisible.value = true
}

async function submitStatus() {
  const id = statusEditingId.value
  const nextStatus = statusDraft.value
  if (id === undefined || nextStatus === undefined) return
  statusSaving.value = true
  try {
    await agreementApi.updateStatus(id, nextStatus)
    ElMessage.success('状态已更新')
    statusDialogVisible.value = false
    load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    statusSaving.value = false
  }
}

async function remove(row: AgreementVo) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」的租约吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await agreementApi.remove(row.id!)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}
</script>
