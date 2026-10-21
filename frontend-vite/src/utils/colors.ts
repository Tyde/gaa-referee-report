import {definePreset} from "@primeuix/themes";
import Lara from "@primeuix/themes/lara";

/*
 * Two colour schemes are served from one preset, selected by
 * `darkModeSelector: 'system'` (see the *App entry points) — i.e. the OS setting.
 *
 * The surface scale is a normal, monotonic ramp: 50 is the lightest (page/panel
 * background) and 950 the darkest (text/header) in *light* mode, and the mirror
 * image of that in *dark* mode. Components may therefore use it the way PrimeVue
 * intends: 50-200 for backgrounds and faint borders, 300-400 for control
 * borders, 500-700 for secondary text, 800-950 for primary text and dark bands.
 * Anything that is not a plain surface role (text, content, form fields, lists,
 * navigation, overlays) is pinned explicitly below, so the palette no longer
 * depends on a token reference falling through to Lara's defaults.
 *
 * Brand: deep teal #0D3134, mint #92ced4 / #d2e8e3, sand #D8B08C / #FFCB9A.
 */
export const RefReportColorPreset = definePreset(Lara, {
    semantic: {
        colorScheme: {
            light: {
                highlight: {
                    background: '#e3efec',
                    focusBackground: '#d3e5e1',
                    color: '#0D3134',
                    focusColor: '#0D3134',
                },

                primary: {
                    color: '#0D3134',
                    contrastColor: '#eafafb',
                    hoverColor: '#154345',
                    activeColor: '#1a5254',
                },

                surface: {
                    0: '#ffffff',
                    50: '#f7faf9',
                    100: '#eef4f2',
                    200: '#e0eae8',
                    300: '#cddcd9',
                    400: '#a8c0bd',
                    500: '#7d9b98',
                    600: '#5c7774',
                    700: '#42605d',
                    800: '#2c4644',
                    900: '#1d3432',
                    950: '#0d3134',
                },

                content: {
                    background: '#ffffff',
                    hoverBackground: '{surface.100}',
                    borderColor: '{surface.200}',
                    color: '#1d3432',
                    hoverColor: '#0d3134',
                },

                text: {
                    color: '#1d3432',
                    hoverColor: '#0d3134',
                    mutedColor: '#5c7774',
                    hoverMutedColor: '#42605d',
                },

                formField: {
                    background: '#ffffff',
                    disabledBackground: '{surface.100}',
                    filledBackground: '{surface.50}',
                    filledFocusBackground: '#ffffff',
                    filledHoverBackground: '{surface.50}',
                    borderColor: '{surface.300}',
                    hoverBorderColor: '{surface.400}',
                    focusBorderColor: '#0D3134',
                    color: '#1d3432',
                    disabledColor: '{surface.500}',
                    placeholderColor: '{surface.700}',
                    floatLabelColor: '{surface.700}',
                    floatLabelFocusColor: '#0D3134',
                    floatLabelActiveColor: '{surface.700}',
                    iconColor: '{surface.700}',
                    shadow: '0 0 0 0.2rem rgba(13, 49, 52, 0.12)',
                },

                list: {
                    option: {
                        color: '#1d3432',
                        focusColor: '#0d3134',
                        selectedColor: '#0d3134',
                        focusBackground: '{surface.100}',
                        selectedBackground: '{surface.200}',
                        selectedFocusBackground: '{surface.300}',
                    },
                    optionGroup: {
                        background: 'transparent',
                        color: '#42605d',
                    },
                },

                navigation: {
                    item: {
                        color: '#42605d',
                        focusColor: '#0d3134',
                        activeColor: '#0d3134',
                        focusBackground: '{surface.100}',
                        activeBackground: '{surface.200}',
                    },
                    submenuLabel: {
                        color: '#42605d',
                    },
                },

                overlay: {
                    select: {
                        background: '#ffffff',
                        borderColor: '{surface.200}',
                        color: '#1d3432',
                    },
                    popover: {
                        background: '#ffffff',
                        borderColor: '{surface.200}',
                        color: '#1d3432',
                    },
                    modal: {
                        background: '#ffffff',
                        borderColor: '{surface.200}',
                        color: '#1d3432',
                    },
                },

                mask: {
                    background: 'rgba(13, 49, 52, 0.45)',
                    color: '#1d3432',
                },

                success: {
                    500: '#22c55e',
                },
                danger: {
                    500: '#ef4444',
                },
                neutral: {
                    500: '#94a3b8', // Used for borders and secondary text
                }
            },
            dark: {

                highlight: {
                    background: '#061724',
                    focusBackground: '{zinc.700}',
                },

                primary: {
                    color: '#92ced4',
                    contrastColor: '#0D3134',
                    hoverColor: '#b0dfe4',
                    activeColor: '#c4e8ec',
                },

                surface: {
                    50: '#effafc',
                    100: '#d7f1f6',
                    200: '#b4e3ed',
                    300: '#81cedf',
                    400: '#46b0ca',
                    500: '#1d3543',
                    600: '#142c37',
                    700: '#0E2129',
                    800: '#061A24',
                    900: '#041723',
                    950: '#03151E',
                },

                content: {
                    background: '#061A24',
                    hoverBackground: '{surface.700}',
                    borderColor: '{surface.600}',
                    color: '#effafc',
                    hoverColor: '#ffffff',
                },

                text: {
                    color: '#effafc',
                    hoverColor: '#ffffff',
                    mutedColor: '#b4e3ed',
                    hoverMutedColor: '#d7f1f6',
                },

                formField: {
                    background: '#0b2430',
                    borderColor: '{surface.600}',
                    hoverBorderColor: '{surface.400}',
                    focusBorderColor: '#92ced4',
                    color: '#effafc',
                    placeholderColor: '#9bb8c4',
                    floatLabelColor: '#b4e3ed',
                    floatLabelFocusColor: '#92ced4',
                    iconColor: '#b4e3ed',
                },

                list: {
                    option: {
                        color: '#effafc',
                        focusColor: '#ffffff',
                        selectedColor: '#ffffff',
                        focusBackground: '{surface.700}',
                        selectedBackground: '{surface.600}',
                        selectedFocusBackground: '{surface.600}',
                    },
                    optionGroup: {
                        background: 'transparent',
                        color: '#b4e3ed',
                    },
                },

                navigation: {
                    item: {
                        color: '#b4e3ed',
                        focusColor: '#ffffff',
                        activeColor: '#ffffff',
                        focusBackground: '{surface.700}',
                        activeBackground: '{surface.600}',
                    },
                },

                overlay: {
                    select: {
                        background: '{surface.800}',
                        borderColor: '{surface.600}',
                        color: '#effafc',
                    },
                    popover: {
                        background: '{surface.800}',
                        borderColor: '{surface.600}',
                        color: '#effafc',
                    },
                    modal: {
                        background: '{surface.800}',
                        borderColor: '{surface.600}',
                        color: '#effafc',
                    },
                },

                mask: {
                    background: 'rgba(3, 21, 30, 0.6)',
                    color: '#effafc',
                },

                success: {
                    500: '#22c55e',
                },
                danger: {
                    500: '#ef4444',
                },
                neutral: {
                    500: '#94a3b8', // Used for borders and secondary text
                },
            }
        }

    },
    components: {
        button: {
            colorScheme: {
                light: {
                    secondary: {
                        background: '{surface.600}',
                        hoverBackground: '{surface.700}',
                        activeBackground: '{surface.800}',
                        borderColor: '{surface.600}',
                        color: '#ffffff',
                        hoverColor: '#ffffff',
                        activeColor: '#ffffff',
                    },
                    info: {
                        background: '#0369a1',
                        hoverBackground: '#075985',
                        activeBackground: '#0c4a6e',
                        borderColor: '#0369a1',
                        hoverBorderColor: '#075985',
                        activeBorderColor: '#0c4a6e',
                        color: '#ffffff',
                        hoverColor: '#ffffff',
                        activeColor: '#ffffff',
                    },
                    danger: {
                        background: '#b91c1c',
                        hoverBackground: '#991b1b',
                        activeBackground: '#7f1d1d',
                        borderColor: '#b91c1c',
                        hoverBorderColor: '#991b1b',
                        activeBorderColor: '#7f1d1d',
                        color: '#ffffff',
                        hoverColor: '#ffffff',
                        activeColor: '#ffffff',
                    },
                    help: {
                        background: '#7e22ce',
                        hoverBackground: '#6b21a8',
                        activeBackground: '#581c87',
                        borderColor: '#7e22ce',
                        hoverBorderColor: '#6b21a8',
                        activeBorderColor: '#581c87',
                        color: '#ffffff',
                        hoverColor: '#ffffff',
                        activeColor: '#ffffff',
                    },
                    success: {
                        background: '#15803d',
                        hoverBackground: '#166534',
                        activeBackground: '#14532d',
                        borderColor: '#15803d',
                        hoverBorderColor: '#166534',
                        activeBorderColor: '#14532d',
                        color: '#ffffff',
                        hoverColor: '#ffffff',
                        activeColor: '#ffffff',
                    },
                    outlined: {
                        secondary: {
                            color: '{surface.700}',
                            borderColor: '{surface.400}',
                            hoverBackground: '{surface.100}',
                            activeBackground: '{surface.200}',
                        },
                    },
                    // Severity groups are resolved per colour scheme at runtime, but the
                    // published typings only describe them under `root`/`outlined`.
                } as Record<string, any>,
            },
        },
        togglebutton: {
            colorScheme: {
                light: {
                    root: {
                        checkedBackground: '{button.primary.background}',
                        checkedColor: '{button.primary.color}'
                    }
                },
                dark: {
                    root: {
                        checkedBackground: '{button.primary.background}',
                        checkedColor: '{button.primary.color}'

                    }
                }
            }
        }
    }

});
