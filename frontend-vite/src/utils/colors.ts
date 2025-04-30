import {definePreset} from "@primeuix/themes";
import Lara from "@primeuix/themes/lara";

export const RefReportColorPreset = definePreset(Lara, {
    semantic: {

        colorScheme: {
            light: {
                highlight: {
                    background: '#f0f9ff',
                    focusBackground: '{zinc.100}',
                },

                primary: {
                    color: '#0D3134',
                    contrastColor: '#92ced4',
                    hoverColor: '#1D5747',
                    activeColor: '#1D5747',
                },

                surface: {
                    50: '#03151E',
                    100: '#041723',
                    200: '#061A24',
                    300: '#0E2129',
                    400: '#143731',
                    500: '#D8B08C',
                    600: '#e3ba9b',
                    700: '#FFCB9A',
                    800: '#D2E8E3',
                    900: '#dcece8',
                    950: '#e6efed',
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
                    hoverColor: '#1D5747',
                    activeColor: '#1D5747',
                    /*
                    contrastColor: string;
                    hoverColor: string;
                    activeColor: string;*/
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
        togglebutton: {
            colorScheme: {
                light: {
                    root: {
                        checkedBackground: '{button.primary.background}',
                        checkedColor: ' {button.primary.color}'
                    }
                },
                dark: {
                    root: {
                        checkedBackground: '{button.primary.background}',
                        checkedColor: ' {button.primary.color}'

                    }
                }
            }
        }
    }

});
