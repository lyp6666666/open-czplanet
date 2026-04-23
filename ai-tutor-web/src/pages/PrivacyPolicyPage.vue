<script setup lang="ts">
import { BRAND_NAME } from '@/constants/brand'

const sections = [
  {
    title: '1. 适用范围',
    paragraphs: [
      `${BRAND_NAME}隐私政策适用于平台网站及相关服务中涉及个人信息处理的场景，包括但不限于注册登录、发布家教需求、浏览教师或机构信息、在线沟通、课程预约、排课管理、在线上课准备、支付相关流程、邮箱提醒与课后总结等。`,
      '如果某项具体服务另有单独说明或授权页面，该说明将与本政策共同构成对个人信息处理的规则。',
    ],
  },
  {
    title: '2. 我们收集的信息',
    paragraphs: [
      '为实现基础功能，我们会根据不同角色收集必要信息，包括手机号、验证码、登录身份、昵称、头像、所在城市等账号资料。',
      '在家教撮合与课程服务过程中，我们可能处理需求描述、授课科目、年级、预算、授课方式、上课时间、地址信息、教师或机构简介、沟通记录、课程安排、支付状态、课后总结与消息提醒设置。',
      '当用户主动填写时，我们还可能处理邮箱地址、课后总结接收邮箱、联系人信息以及申诉或客服反馈内容。对于摄像头、麦克风等设备权限，仅在线上课堂准备或上课场景中按需调用。',
    ],
  },
  {
    title: '3. 我们如何使用这些信息',
    paragraphs: [
      '我们使用相关信息完成身份验证、账户登录、需求发布、信息匹配、沟通协同、课程排期、订单处理、消息通知、邮箱提醒、课后总结生成与服务安全保障。',
      '我们也会基于运营需要进行异常排查、纠纷处理、服务质量分析和功能优化，但会尽量采用去标识化、最小必要范围或聚合后的方式进行。',
    ],
  },
  {
    title: '4. 联系方式与敏感信息保护',
    paragraphs: [
      '平台对手机号、邮箱、具体地址、聊天记录、课程信息等采取分级保护。联系方式不会向无业务关系的用户直接公开，只有在达成相应业务条件、完成必要流程或基于用户明确操作时，才会在对应场景中展示或使用。',
      '如涉及未成年人学习信息，我们将仅在提供教学服务、课程协同和家长通知等必要范围内处理，并尽量减少展示与传播范围。',
    ],
  },
  {
    title: '5. 信息共享与披露',
    paragraphs: [
      '我们不会将个人信息出售给任何第三方。仅在实现平台核心服务所必要时，才会与支付、云存储、消息通知、直播课堂等合作方共享所需的最少信息，并要求其履行保密和安全义务。',
      '在法律法规要求、监管机关依法要求，或为保护平台、用户及公众的人身财产安全所必需时，我们可能依法披露相关信息。',
    ],
  },
  {
    title: '6. 信息存储与安全',
    paragraphs: [
      '我们会在实现服务目的所需的期限内保存个人信息，并采取访问控制、传输加密、日志审计、权限隔离等合理安全措施，降低信息被泄露、篡改或丢失的风险。',
      '尽管如此，互联网环境并非绝对安全。如发生可能影响个人权益的安全事件，我们将按照法律法规要求及时告知并采取补救措施。',
    ],
  },
  {
    title: '7. 您的权利',
    paragraphs: [
      '您可以根据平台功能对个人资料、需求信息、课程安排、邮箱提醒等进行查询、修改或删除；也可以通过退出登录、停用相关功能等方式减少信息处理范围。',
      '若您希望注销账号、撤回授权、删除特定信息或就隐私问题提出异议，可通过平台站内反馈、客服渠道或后续公布的官方联系方式与我们联系。',
    ],
  },
  {
    title: '8. 政策更新',
    paragraphs: [
      '当业务功能、法律要求或个人信息处理方式发生变化时，我们可能更新本政策。更新后会通过站内页面、公示通知等合理方式提示，重大变更将以更显著方式告知。',
      '本政策自 2026 年 4 月 22 日起生效。',
    ],
  },
]
</script>

<template>
  <div class="page">
    <section class="hero">
      <div class="eyebrow">
        PRIVACY POLICY
      </div>
      <h1>{{ BRAND_NAME }}隐私政策</h1>
      <p>
        我们重视用户信息安全，并坚持以最小必要、明确目的、合理授权的方式处理个人信息。以下内容用于说明平台在家教撮合、课程协同与教学服务中如何收集、使用和保护您的信息。
      </p>
      <div class="tag">
        生效日期：2026 年 4 月 22 日
      </div>
    </section>

    <section class="card document">
      <article
        v-for="section in sections"
        :key="section.title"
        class="block"
      >
        <h2>{{ section.title }}</h2>
        <p
          v-for="paragraph in section.paragraphs"
          :key="paragraph"
        >
          {{ paragraph }}
        </p>
      </article>
    </section>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 40px 20px 56px;
  background:
    radial-gradient(circle at top right, rgba(0, 190, 189, 0.12), transparent 28%),
    linear-gradient(180deg, #f9fbfb 0%, #f6f7fb 45%, #f6f7fb 100%);
}

.hero,
.document {
  width: min(940px, 100%);
  margin: 0 auto;
}

.hero {
  padding: 36px 8px 24px;
}

.eyebrow {
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

h1 {
  margin: 10px 0 14px;
  font-size: clamp(30px, 4vw, 46px);
  line-height: 1.12;
}

.hero p {
  margin: 0;
  max-width: 760px;
  color: var(--muted);
  font-size: 16px;
  line-height: 1.9;
}

.tag {
  display: inline-flex;
  align-items: center;
  margin-top: 18px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(0, 190, 189, 0.08);
  color: var(--primary);
  font-size: 13px;
  font-weight: 600;
}

.document {
  padding: 12px 30px;
  border-radius: 20px;
}

.block {
  padding: 22px 0;
  border-bottom: 1px solid rgba(31, 35, 41, 0.08);
}

.block:last-child {
  border-bottom: none;
}

h2 {
  margin: 0 0 12px;
  font-size: 20px;
}

p {
  margin: 0 0 10px;
  color: var(--muted);
  line-height: 1.9;
  font-size: 15px;
}

p:last-child {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .page {
    padding: 24px 14px 40px;
  }

  .document {
    padding: 8px 18px;
  }

  .block {
    padding: 18px 0;
  }
}
</style>
