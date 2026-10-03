(function () {
    if (window._customPtrLoaded) return;
    window._customPtrLoaded = true;

    const PULL_THRESHOLD = 70; 
    let startY = 0;
    let currentY = 0;
    let pulling = false;
    let refreshing = false;

    // Kreiramo vizuelni indikator (loader) na vrhu
    let ptrElement = document.createElement('div');
    ptrElement.className = 'custom-ptr';
    ptrElement.innerHTML = '<div class="ptr-spinner"></div>';
    ptrElement.style.cssText = `
        position: fixed;
        top: -50px;
        left: 0;
        width: 100%;
        height: 50px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #111;
        border-bottom: 1px solid #222;
        transition: transform 0.2s ease;
        z-index: 99999;
        pointer-events: none;
    `;

    // Dodajemo stil za spinner
    const styleTag = document.createElement('style');
    styleTag.innerHTML = `
        .ptr-spinner {
            width: 22px;
            height: 22px;
            border: 2px solid #444;
            border-top-color: #007aff;
            border-radius: 50%;
            animation: ptr-spin 0.8s linear infinite;
        }
        @keyframes ptr-spin {
            to { transform: rotate(360deg); }
        }
    `;
    document.head.appendChild(styleTag);

    if (document.body) {
        document.body.appendChild(ptrElement);
    } else {
        document.addEventListener('DOMContentLoaded', () => {
            document.body.appendChild(ptrElement);
        });
    }

    // Provera da li je element ili neki roditelj skrolabilan i pomeren sa vrha
    function isAnyParentScrollableUp(el) {
        let curr = el;
        while (curr && curr !== document.body && curr !== document.documentElement) {
            const style = window.getComputedStyle(curr);
            const overflowY = style.getPropertyValue('overflow-y');
            const isScrollable = (overflowY === 'auto' || overflowY === 'scroll' || overflowY === 'overlay');
            
            if (isScrollable && curr.scrollHeight > curr.clientHeight) {
                if (curr.scrollTop > 0) {
                    return true;
                }
            }
            curr = curr.parentElement;
        }
        return false;
    }

    // 1. TOUCH START
    window.addEventListener('touchstart', function (e) {
        if (refreshing) return;

        if (window.scrollY > 0) {
            pulling = false;
            return;
        }

        if (isAnyParentScrollableUp(e.target)) {
            pulling = false;
            return;
        }

        startY = e.touches[0].clientY;
        pulling = true;
    }, { passive: true });

    // 2. TOUCH MOVE
    window.addEventListener('touchmove', function (e) {
        if (!pulling || refreshing) return;

        currentY = e.touches[0].clientY;
        const diff = currentY - startY;

        if (diff > 0 && window.scrollY === 0) {
            const pullDistance = Math.min(diff * 0.4, 100); 
            ptrElement.style.transform = `translateY(${pullDistance}px)`;
        }
    }, { passive: true });

    // 3. TOUCH END
    window.addEventListener('touchend', function () {
        if (!pulling || refreshing) return;
        pulling = false;

        const diff = currentY - startY;
        const pullDistance = Math.min(diff * 0.4, 100);

        if (pullDistance >= PULL_THRESHOLD && window.scrollY === 0) {
            refreshing = true;
            ptrElement.style.transform = `translateY(50px)`;
            
            // Osvežavanje stranice
            setTimeout(function () {
                window.location.reload(); 
            }, 400);
        } else {
            ptrElement.style.transform = `translateY(0px)`;
        }
        
        startY = 0;
        currentY = 0;
    });
})();
