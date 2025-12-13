/// UI Module for Plus/4 Emulator
/// Provides egui-based overlay menu for emulator control

pub mod file_browser;
pub mod state;

use egui_macroquad::egui::{self, Align2, Context, Vec2, Window};
use state::{EmulatorAction, UiState};

/// Main emulator UI controller
pub struct EmulatorUi {
    pub state: UiState,
}

impl EmulatorUi {
    pub fn new() -> Self {
        Self {
            state: UiState::new(),
        }
    }

    /// Render the UI overlay
    pub fn render(&mut self, ctx: &Context) {
        if !self.state.visible {
            return;
        }

        // Semi-transparent overlay background
        egui::Area::new(egui::Id::new("overlay_background"))
            .fixed_pos([0.0, 0.0])
            .show(ctx, |ui| {
                let screen_rect = ui.ctx().screen_rect();
                ui.painter().rect_filled(
                    screen_rect,
                    0.0,
                    egui::Color32::from_rgba_unmultiplied(0, 0, 0, 180),
                );
            });

        // Main menu window
        Window::new("Plus/4 Emulator")
            .anchor(Align2::CENTER_CENTER, Vec2::ZERO)
            .collapsible(false)
            .resizable(false)
            .min_width(400.0)
            .show(ctx, |ui| {
                self.render_main_menu(ui);
            });
    }

    fn render_main_menu(&mut self, ui: &mut egui::Ui) {
        ui.heading("Emulator Controls");
        ui.separator();

        ui.horizontal(|ui| {
            if ui.button("Reset (F11)").clicked() {
                self.state.pending_action = Some(EmulatorAction::Reset);
                self.state.visible = false;
            }

            if ui.button("Quit").clicked() {
                self.state.pending_action = Some(EmulatorAction::Quit);
            }

            if ui.button("Close (ESC)").clicked() {
                self.state.visible = false;
            }
        });

        ui.add_space(16.0);

        // File browser section
        egui::CollapsingHeader::new("Load PRG File")
            .default_open(true)
            .show(ui, |ui| {
                file_browser::render_file_browser(ui, &mut self.state);
            });

        ui.add_space(8.0);

        // Future sections placeholder (disabled)
        ui.add_enabled_ui(false, |ui| {
            egui::CollapsingHeader::new("CPU Monitor (Coming Soon)").show(ui, |ui| {
                ui.label("CPU state and disassembly will appear here");
            });

            egui::CollapsingHeader::new("Memory Viewer (Coming Soon)").show(ui, |ui| {
                ui.label("Memory inspection tools will appear here");
            });
        });

        ui.add_space(8.0);
        ui.separator();
        ui.label("Press ESC to toggle this menu");
    }

    /// Check if UI wants input focus (to disable emulator keyboard)
    pub fn wants_keyboard(&self) -> bool {
        self.state.visible
    }

    /// Toggle UI visibility
    pub fn toggle(&mut self) {
        self.state.toggle_visibility();
    }

    /// Take any pending action
    pub fn take_action(&mut self) -> Option<EmulatorAction> {
        self.state.take_action()
    }
}

impl Default for EmulatorUi {
    fn default() -> Self {
        Self::new()
    }
}
