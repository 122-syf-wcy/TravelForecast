import { request } from './request'

export const fetchSpotDetail = (id) =>
  request({ url: `/spots/${id}` })

export const fetchSubSpots = (id) =>
  request({ url: `/spots/${id}/sub-spots` })
