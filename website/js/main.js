/**
 * UK Self-Employment Manager - Landing Page JavaScript
 * Version: 1.0.0
 *
 * Features:
 * - Mobile navigation toggle
 * - Smooth scroll navigation
 * - Sticky header scroll effects
 * - FAQ accordion (enhanced)
 * - Download link tracking (placeholder)
 */

(function() {
    'use strict';

    // ===== DOM Elements =====
    const mobileMenuToggle = document.getElementById('mobile-menu-toggle');
    const navLinks = document.getElementById('nav-links');
    const navHeader = document.querySelector('.nav-header');
    const faqItems = document.querySelectorAll('.faq-item');

    // ===== Mobile Navigation =====
    function initMobileNav() {
        if (!mobileMenuToggle || !navLinks) return;

        mobileMenuToggle.addEventListener('click', function() {
            const isExpanded = this.getAttribute('aria-expanded') === 'true';

            this.setAttribute('aria-expanded', !isExpanded);
            navLinks.classList.toggle('active');

            // Prevent body scroll when menu is open
            document.body.style.overflow = navLinks.classList.contains('active') ? 'hidden' : '';
        });

        // Close menu when clicking a link
        navLinks.querySelectorAll('a').forEach(function(link) {
            link.addEventListener('click', function() {
                mobileMenuToggle.setAttribute('aria-expanded', 'false');
                navLinks.classList.remove('active');
                document.body.style.overflow = '';
            });
        });

        // Close menu on escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && navLinks.classList.contains('active')) {
                mobileMenuToggle.setAttribute('aria-expanded', 'false');
                navLinks.classList.remove('active');
                document.body.style.overflow = '';
                mobileMenuToggle.focus();
            }
        });

        // Close menu when clicking outside
        document.addEventListener('click', function(e) {
            if (navLinks.classList.contains('active') &&
                !navLinks.contains(e.target) &&
                !mobileMenuToggle.contains(e.target)) {
                mobileMenuToggle.setAttribute('aria-expanded', 'false');
                navLinks.classList.remove('active');
                document.body.style.overflow = '';
            }
        });
    }

    // ===== Smooth Scroll Navigation =====
    function initSmoothScroll() {
        document.querySelectorAll('a[href^="#"]').forEach(function(anchor) {
            anchor.addEventListener('click', function(e) {
                const targetId = this.getAttribute('href');

                // Skip if it's just "#" or empty
                if (targetId === '#' || targetId === '') return;

                const targetElement = document.querySelector(targetId);

                if (targetElement) {
                    e.preventDefault();

                    const navHeight = navHeader ? navHeader.offsetHeight : 72;
                    const targetPosition = targetElement.getBoundingClientRect().top + window.pageYOffset - navHeight;

                    window.scrollTo({
                        top: targetPosition,
                        behavior: 'smooth'
                    });

                    // Update URL hash without jumping
                    if (history.pushState) {
                        history.pushState(null, null, targetId);
                    }

                    // Focus management for accessibility
                    targetElement.setAttribute('tabindex', '-1');
                    targetElement.focus({ preventScroll: true });
                }
            });
        });
    }

    // ===== Sticky Header Scroll Effect =====
    function initStickyHeader() {
        if (!navHeader) return;

        let lastScrollTop = 0;
        let ticking = false;

        function updateHeader(scrollTop) {
            // Add shadow when scrolled
            if (scrollTop > 10) {
                navHeader.style.boxShadow = '0 2px 10px rgba(0, 0, 0, 0.1)';
            } else {
                navHeader.style.boxShadow = '0 2px 4px rgba(0, 0, 0, 0.04)';
            }

            lastScrollTop = scrollTop;
            ticking = false;
        }

        window.addEventListener('scroll', function() {
            if (!ticking) {
                window.requestAnimationFrame(function() {
                    updateHeader(window.pageYOffset || document.documentElement.scrollTop);
                });
                ticking = true;
            }
        }, { passive: true });
    }

    // ===== FAQ Accordion Enhancement =====
    function initFaqAccordion() {
        if (!faqItems.length) return;

        faqItems.forEach(function(item) {
            const summary = item.querySelector('summary');

            if (summary) {
                // Enhanced keyboard navigation
                summary.addEventListener('keydown', function(e) {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        this.click();
                    }
                });

                // Optional: Close other items when one is opened (accordion behavior)
                // Uncomment the following to enable single-open mode:
                /*
                item.addEventListener('toggle', function() {
                    if (this.open) {
                        faqItems.forEach(function(otherItem) {
                            if (otherItem !== item && otherItem.open) {
                                otherItem.open = false;
                            }
                        });
                    }
                });
                */
            }
        });
    }

    // ===== Download Link Tracking (Analytics Placeholder) =====
    function initDownloadTracking() {
        const downloadCards = document.querySelectorAll('.download-card');

        downloadCards.forEach(function(card) {
            card.addEventListener('click', function(e) {
                const platform = this.querySelector('h3')?.textContent || 'Unknown';
                const format = this.querySelector('.download-format')?.textContent || 'Unknown';

                // Analytics tracking placeholder
                // Replace with your analytics implementation (e.g., Google Analytics, Plausible)
                if (typeof gtag === 'function') {
                    gtag('event', 'download', {
                        'event_category': 'Downloads',
                        'event_label': platform,
                        'value': 1
                    });
                }

                // Console log for development
                console.log('[Download]', { platform: platform, format: format });

                // Note: Don't prevent default - let the download proceed
            });
        });
    }

    // ===== External Link Handling =====
    function initExternalLinks() {
        // Add security attributes to external links if not already present
        document.querySelectorAll('a[href^="http"]').forEach(function(link) {
            // Check if it's truly external (different origin)
            try {
                const linkUrl = new URL(link.href);
                if (linkUrl.origin !== window.location.origin) {
                    // Already has target="_blank"? Ensure security attributes
                    if (link.getAttribute('target') === '_blank') {
                        if (!link.getAttribute('rel')?.includes('noopener')) {
                            const rel = link.getAttribute('rel') || '';
                            link.setAttribute('rel', (rel + ' noopener noreferrer').trim());
                        }
                    }
                }
            } catch (e) {
                // Invalid URL, skip
            }
        });
    }

    // ===== Intersection Observer for Animations (Future Enhancement) =====
    function initScrollAnimations() {
        // Check for reduced motion preference
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            return;
        }

        // Observer for fade-in animations could be added here
        // Currently disabled for simplicity and performance
    }

    // ===== Update Copyright Year =====
    function updateCopyrightYear() {
        const copyrightElements = document.querySelectorAll('.footer-bottom');
        const currentYear = new Date().getFullYear();

        // The HTML already has 2026, but this ensures it stays current
        copyrightElements.forEach(function(el) {
            el.innerHTML = el.innerHTML.replace(/\d{4}(?=\s*UK Self-Employment Manager)/, currentYear);
        });
    }

    // ===== App Tour Tab Navigation =====
    function initAppTour() {
        var tabList = document.querySelector('.demo-app-tabs');
        if (!tabList) return;

        var tabs = tabList.querySelectorAll('.demo-app-tab');
        var panels = document.querySelectorAll('.demo-app-panel');
        var autoAdvanceTimer = null;
        var AUTO_ADVANCE_DELAY = 5000;

        // Check reduced motion preference
        var prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        function switchTab(newTab) {
            // Deactivate all tabs and panels
            tabs.forEach(function(t) {
                t.classList.remove('active');
                t.setAttribute('aria-selected', 'false');
                t.setAttribute('tabindex', '-1');
            });
            panels.forEach(function(p) {
                p.classList.remove('active');
            });

            // Activate new tab
            newTab.classList.add('active');
            newTab.setAttribute('aria-selected', 'true');
            newTab.setAttribute('tabindex', '0');

            // Show corresponding panel
            var panelId = newTab.getAttribute('aria-controls');
            var panel = document.getElementById(panelId);
            if (panel) panel.classList.add('active');
        }

        // Click handler
        tabs.forEach(function(tab) {
            tab.addEventListener('click', function() {
                switchTab(tab);
                stopAutoAdvance();
            });
        });

        // Keyboard navigation (Arrow keys, Home, End)
        tabList.addEventListener('keydown', function(e) {
            var currentIndex = Array.from(tabs).indexOf(document.activeElement);
            var newIndex;

            if (e.key === 'ArrowRight') {
                newIndex = (currentIndex + 1) % tabs.length;
            } else if (e.key === 'ArrowLeft') {
                newIndex = (currentIndex - 1 + tabs.length) % tabs.length;
            } else if (e.key === 'Home') {
                newIndex = 0;
            } else if (e.key === 'End') {
                newIndex = tabs.length - 1;
            } else {
                return;
            }

            e.preventDefault();
            switchTab(tabs[newIndex]);
            tabs[newIndex].focus();
            stopAutoAdvance();
        });

        // Auto-advance (respects reduced motion)
        function startAutoAdvance() {
            if (prefersReducedMotion) return;
            autoAdvanceTimer = setInterval(function() {
                var currentIndex = Array.from(tabs).findIndex(function(t) {
                    return t.classList.contains('active');
                });
                var nextIndex = (currentIndex + 1) % tabs.length;
                switchTab(tabs[nextIndex]);
            }, AUTO_ADVANCE_DELAY);
        }

        function stopAutoAdvance() {
            if (autoAdvanceTimer) {
                clearInterval(autoAdvanceTimer);
                autoAdvanceTimer = null;
            }
        }

        startAutoAdvance();
    }

    // ===== OS Detection & Smart Download Highlighting (SE-1604) =====
    function detectOS() {
        // Try modern API first (Chromium browsers)
        if (navigator.userAgentData && navigator.userAgentData.platform) {
            var platform = navigator.userAgentData.platform.toLowerCase();
            if (platform.includes('windows')) return 'windows';
            if (platform.includes('macos') || platform.includes('mac')) return 'macos';
            if (platform.includes('linux')) return 'linux';
        }

        // Fallback to navigator.platform + userAgent
        var platform = (navigator.platform || '').toLowerCase();
        var ua = (navigator.userAgent || '').toLowerCase();

        if (platform.includes('win') || ua.includes('windows')) return 'windows';
        if (platform.includes('mac') || ua.includes('macintosh') || ua.includes('mac os')) return 'macos';
        if (platform.includes('linux') || ua.includes('linux')) return 'linux';

        return 'unknown';
    }

    function initSmartDownload() {
        var os = detectOS();
        if (os === 'unknown') return;

        var downloadCards = document.querySelectorAll('.download-card[data-os]');
        downloadCards.forEach(function(card) {
            if (card.getAttribute('data-os') === os) {
                card.classList.add('recommended');
            }
        });

        // Update hero CTA button text
        var heroBtn = document.getElementById('hero-download-btn');
        if (heroBtn) {
            var osNames = { windows: 'Windows', macos: 'macOS', linux: 'Linux' };
            var osName = osNames[os];
            if (osName) {
                heroBtn.childNodes.forEach(function(node) {
                    if (node.nodeType === 3 && node.textContent.trim()) {
                        node.textContent = '\n                            Download for ' + osName + '\n                        ';
                    }
                });
            }
        }

        // Update install guide tabs if on install page
        var installTabs = document.querySelectorAll('.install-tab[data-os]');
        installTabs.forEach(function(tab) {
            if (tab.getAttribute('data-os') === os) {
                // Activate this tab
                document.querySelectorAll('.install-tab').forEach(function(t) {
                    t.classList.remove('active');
                    t.setAttribute('aria-selected', 'false');
                    t.setAttribute('tabindex', '-1');
                });
                document.querySelectorAll('.install-panel').forEach(function(p) {
                    p.classList.remove('active');
                });

                tab.classList.add('active');
                tab.setAttribute('aria-selected', 'true');
                tab.setAttribute('tabindex', '0');

                var panelId = tab.getAttribute('aria-controls');
                var panel = document.getElementById(panelId);
                if (panel) panel.classList.add('active');
            }
        });
    }

    // ===== Dynamic Download Links from GitHub Releases API (SE-1605) =====
    function initDynamicDownloads() {
        var GITHUB_API = 'https://api.github.com/repos/olehsvyrydov/selfemploy-uk/releases/latest';

        fetch(GITHUB_API)
            .then(function(response) {
                if (!response.ok) throw new Error('API request failed');
                return response.json();
            })
            .then(function(release) {
                var version = release.tag_name;
                var assets = release.assets || [];

                // Update download card links
                var assetMap = {};
                assets.forEach(function(asset) {
                    var name = asset.name.toLowerCase();
                    if (name.endsWith('.msi')) assetMap.windows = asset.browser_download_url;
                    if (name.endsWith('.dmg')) assetMap.macos = asset.browser_download_url;
                    if (name.endsWith('.deb')) assetMap.linux = asset.browser_download_url;
                });

                var downloadCards = document.querySelectorAll('.download-card[data-os]');
                downloadCards.forEach(function(card) {
                    var os = card.getAttribute('data-os');
                    if (assetMap[os]) {
                        card.setAttribute('href', assetMap[os]);
                    }
                });

                // Update version tag
                var versionTag = document.getElementById('download-version');
                if (versionTag && version) {
                    versionTag.textContent = version;
                }

                // Update release date
                var dateEl = document.getElementById('download-date');
                if (dateEl && release.published_at) {
                    var date = new Date(release.published_at);
                    var months = ['January', 'February', 'March', 'April', 'May', 'June',
                                  'July', 'August', 'September', 'October', 'November', 'December'];
                    dateEl.textContent = 'Released ' + months[date.getMonth()] + ' ' + date.getFullYear();
                }

                // Update JSON-LD structured data
                var ldScript = document.querySelector('script[type="application/ld+json"]');
                if (ldScript) {
                    try {
                        var ld = JSON.parse(ldScript.textContent);
                        if (version) ld.softwareVersion = version.replace(/^v/, '');
                        if (release.html_url) ld.downloadUrl = release.html_url;
                        ldScript.textContent = JSON.stringify(ld, null, 8);
                    } catch (e) {
                        // Ignore JSON parse errors
                    }
                }
            })
            .catch(function() {
                // On failure (rate limit, no releases, network error), keep static fallback links
            });
    }

    // ===== Install Guide Tab Navigation =====
    function initInstallTabs() {
        var tabList = document.querySelector('.install-tabs');
        if (!tabList) return;

        var tabs = tabList.querySelectorAll('.install-tab');
        var panels = document.querySelectorAll('.install-panel');

        tabs.forEach(function(tab) {
            tab.addEventListener('click', function() {
                tabs.forEach(function(t) {
                    t.classList.remove('active');
                    t.setAttribute('aria-selected', 'false');
                    t.setAttribute('tabindex', '-1');
                });
                panels.forEach(function(p) { p.classList.remove('active'); });

                tab.classList.add('active');
                tab.setAttribute('aria-selected', 'true');
                tab.setAttribute('tabindex', '0');

                var panelId = tab.getAttribute('aria-controls');
                var panel = document.getElementById(panelId);
                if (panel) panel.classList.add('active');
            });
        });

        // Keyboard navigation
        tabList.addEventListener('keydown', function(e) {
            var currentIndex = Array.from(tabs).indexOf(document.activeElement);
            var newIndex;

            if (e.key === 'ArrowRight') {
                newIndex = (currentIndex + 1) % tabs.length;
            } else if (e.key === 'ArrowLeft') {
                newIndex = (currentIndex - 1 + tabs.length) % tabs.length;
            } else {
                return;
            }

            e.preventDefault();
            tabs[newIndex].click();
            tabs[newIndex].focus();
        });
    }

    // ===== Initialize All Features =====
    function init() {
        initMobileNav();
        initSmoothScroll();
        initStickyHeader();
        initFaqAccordion();
        initDownloadTracking();
        initExternalLinks();
        initScrollAnimations();
        initAppTour();
        initSmartDownload();
        initDynamicDownloads();
        initInstallTabs();

        // Log initialization in development
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            console.log('[UK Self-Employment Manager] Landing page initialized');
        }
    }

    // ===== Run on DOM Ready =====
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
