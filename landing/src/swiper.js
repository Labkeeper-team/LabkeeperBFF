import Swiper from 'swiper';
import { Navigation } from 'swiper/modules';

window.Swiper = function SwiperElement(element, options = {}) {
    return new Swiper(element, {
        modules: [Navigation],
        ...options,
    });
};
