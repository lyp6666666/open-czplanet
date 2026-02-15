export interface BaseResponse<T> {
  code: number
  data: T
  message: string
}

export interface CursorPageRequest {
  pageSize?: number
  cursor?: number | null
}

export interface CursorPageResponse<T> {
  nextCursor: number | null
  isLast: boolean
  list: T[]
}

export interface SubjectTreeNode {
  id: number
  parentId: number
  name: string
  grade?: string
  description?: string
  children: SubjectTreeNode[]
}

export interface HomeConfigVO {
  defaultCity: string
  citySelectable: boolean
  search: null | {
    placeholder: string
    defaultMode: string
  }
  nav: Array<{
    key: string
    name: string
    link: string
  }>
  authEntry: null | {
    loginText: string
    link: string
  }
}

export interface GeoLocateVO {
  ip: string
  city: string
  province: string
  cityCode: string
  suggestCities: string[]
}

export interface HotWordsVO {
  updatedAt: string
  list: Array<{
    word: string
    type: string
  }>
}

export interface SearchSuggestVO {
  q: string
  list: Array<{
    type: string
    title: string
    subtitle: string
    payload: Record<string, unknown>
  }>
}

export interface BannersVO {
  carousel: Array<{
    id: string
    title: string
    subtitle: string
    imageUrl: string
    link: {
      type: string
      url: string
    }
  }>
  cards: Array<{
    id: string
    title: string
    subtitle: string
    imageUrl: string
    link: {
      type: string
      url: string
    }
  }>
}

export interface HotTabsVO {
  type: string
  tabs: Array<{
    tabId: string
    name: string
    params: Record<string, unknown>
  }>
}

export interface HotServiceCardVO {
  serviceId: number
  title: string
  subject: { id: number; name: string }
  pricePerHour: string
  mode: string
  city: string
  tutor: {
    userId: number
    displayName: string
    avatar: string
    education: string
    experienceYears: number
    ratePerHour: string
  }
  tags: string[]
}

export interface HotDemandCardVO {
  demandId: number
  title: string
  subject: { id: number; name: string }
  budget: { min: string; max: string; unit: string }
  classMode: string
  city: string
  addressSimple: string
  childAge: number
  scheduleText: string
  parent: { userId: number; displayName: string; avatar: string }
  tags: string[]
}

export interface HotTutorCardVO {
  userId: number
  displayName: string
  avatar: string
  city: string
  education: string
  experienceYears: number
  ratePerHour: string
  subjectTags: string[]
  highlights: string[]
  representativeServices: Array<{
    serviceId: number
    title: string
    pricePerHour: string
  }>
}

export interface FooterLinksVO {
  links: Array<{ name: string; url: string }>
}
