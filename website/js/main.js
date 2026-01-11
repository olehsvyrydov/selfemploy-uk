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

    // ===== Initialize All Features =====
    function init() {
        initMobileNav();
        initSmoothScroll();
        initStickyHeader();
        initFaqAccordion();
        initDownloadTracking();
        initExternalLinks();
        initScrollAnimations();

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
