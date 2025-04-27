import {definePreset} from "@primeuix/themes";
import Lara from "@primeuix/themes/lara";

export const RefReportColorPreset = definePreset(Lara, {
    semantic: {

        colorScheme: {
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
                }
            }
        }
    }
});
