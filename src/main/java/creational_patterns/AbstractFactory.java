package creational_patterns;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

enum Platform {
    Windows, Mac, Linux
}

@FunctionalInterface
interface Button {
    void render();
}

interface CheckBox {
    void toggle();

    void render();

    boolean isChecked();
}

interface Dialog {
    void render();
    void addButton(Button button);
    void addCheckbox(CheckBox checkbox);
}

interface UIFactory {
    Button createButton(String label);
    CheckBox createCheckBox(String label);
    Dialog createDialog(String title);
}

// Buttons

final class WindowsButton implements Button {

    private final String label;

    public WindowsButton(String label){
        this.label = Objects.requireNonNull(label, "Button label cannot be null");
    }

    @Override
    public void render() {
        System.out.println("Rendering [Windows] button: " + label);
    }
}

final class MacButton implements Button {

    private final String label;

    public MacButton(String label){
        this.label = Objects.requireNonNull(label, "Button label cannot be null");
    }

    @Override
    public void render() {
        System.out.println("Rendering [Mac] button: " + label);
    }
}

final class LinuxButton implements Button {

    private final String label;

    public LinuxButton(String label){
        this.label = Objects.requireNonNull(label, "Button label cannot be null");
    }

    @Override
    public void render() {
        System.out.println("Rendering [Linux] button: " + label);
    }
}

// Checkboxes

final class WindowsCheckBox implements CheckBox {

    private boolean checked;
    private final String label;

    public WindowsCheckBox(String label){
        this.label = Objects.requireNonNull(label, "CheckBox label cannot be null");
        this.checked = false;
    }

    @Override
    public void toggle() {
        this.checked = !this.checked;
    }

    @Override
    public void render() {
        System.out.println("Rendering [Windows] checkbox: " + label + (isChecked() ? " [x]" : " [ ]"));
    }

    @Override
    public boolean isChecked() {
        return checked;
    }
}

final class MacCheckBox implements CheckBox {

    private boolean checked;
    private final String label;

    public MacCheckBox(String label){
        this.label = Objects.requireNonNull(label, "CheckBox label cannot be null");
        this.checked = false;
    }

    @Override
    public void toggle() {
        this.checked = !this.checked;
    }

    @Override
    public void render() {
        System.out.println("Rendering [Mac] checkbox: " + label + (isChecked() ? " [x]" : " [ ]"));
    }

    @Override
    public boolean isChecked() {
        return checked;
    }
}

final class LinuxCheckBox implements CheckBox {

    private boolean checked;
    private final String label;

    public LinuxCheckBox(String label){
        this.label = Objects.requireNonNull(label, "CheckBox label cannot be null");
        this.checked = false;
    }

    @Override
    public void toggle() {
        this.checked = !this.checked;
    }

    @Override
    public void render() {
        System.out.println("Rendering [Linux] checkbox: " + label + (isChecked() ? " [x]" : " [ ]"));
    }

    @Override
    public boolean isChecked() {
        return checked;
    }
}

// Dialogs

final class WindowsDialog implements Dialog{

    private final List<WindowsButton> buttons;
    private final List<WindowsCheckBox> checkboxes;
    private final String title;

    WindowsDialog(String title){
        this.title = title;
        this.buttons = new ArrayList<>();
        this.checkboxes = new ArrayList<>();
    }

    @Override
    public void render() {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Rendering [Windows] dialog: " + title);
        for (Button button : buttons) {
            button.render();
        }
        for (CheckBox checkbox : checkboxes) {
            checkbox.render();
        }
        System.out.println("----------------------------------------------------------------");
    }

    @Override
    public void addButton(Button button) {
        if(!(button instanceof WindowsButton)){
            throw new IllegalArgumentException("Invalid button type. WindowsButton expected.");
        }
        this.buttons.add((WindowsButton) button);
    }

    @Override
    public void addCheckbox(CheckBox checkbox) {
        if(!(checkbox instanceof WindowsCheckBox)){
            throw new IllegalArgumentException("Invalid checkbox type. WindowsCheckBox expected.");
        }
        this.checkboxes.add((WindowsCheckBox) checkbox);
    }
}

final class MacDialog implements Dialog{

    private final List<MacButton> buttons;
    private final List<MacCheckBox> checkboxes;

    private final String title;

    MacDialog(String title){
        this.title = title;
        this.buttons = new ArrayList<>();
        this.checkboxes = new ArrayList<>();
    }

    @Override
    public void render() {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Rendering [Mac] dialog: " + title);
        for (Button button : buttons) {
            button.render();
        }
        for (CheckBox checkbox : checkboxes) {
            checkbox.render();
        }
        System.out.println("----------------------------------------------------------------");
    }

    @Override
    public void addButton(Button button) {
        if(!(button instanceof MacButton)){
            throw new IllegalArgumentException("Invalid button type. MacButton expected.");
        }
        this.buttons.add((MacButton) button);
    }

    @Override
    public void addCheckbox(CheckBox checkbox) {
        if(!(checkbox instanceof MacCheckBox)){
            throw new IllegalArgumentException("Invalid checkbox type. MacCheckBox expected.");
        }
        this.checkboxes.add((MacCheckBox) checkbox);
    }
}

final class LinuxDialog implements Dialog{

    private final List<LinuxButton> buttons;
    private final List<LinuxCheckBox> checkboxes;
    private final String title;

    LinuxDialog(String title){
        this.title = title;
        this.buttons = new ArrayList<>();
        this.checkboxes = new ArrayList<>();
    }

    @Override
    public void render() {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Rendering [Linux] dialog: " + title);
        for (Button button : buttons) {
            button.render();
        }
        for (CheckBox checkbox : checkboxes) {
            checkbox.render();
        }
        System.out.println("----------------------------------------------------------------");
    }

    @Override
    public void addButton(Button button) {
        if(!(button instanceof LinuxButton)){
            throw new IllegalArgumentException("Invalid button type. LinuxButton expected.");
        }
        this.buttons.add((LinuxButton) button);
    }

    @Override
    public void addCheckbox(CheckBox checkbox) {
        if(!(checkbox instanceof LinuxCheckBox)){
            throw new IllegalArgumentException("Invalid checkbox type. LinuxCheckBox expected.");
        }
        this.checkboxes.add((LinuxCheckBox) checkbox);
    }
}



// UI Factories

final class WindowsUIFactory implements UIFactory{

    @Override
    public Button createButton(String label) {
        return new WindowsButton(label);
    }

    @Override
    public CheckBox createCheckBox(String label) {
        return new WindowsCheckBox(label);
    }

    @Override
    public Dialog createDialog(String title) {
        return new WindowsDialog(title);
    }
}

final class MacUIFactory implements UIFactory{

    @Override
    public Button createButton(String label) {
        return new MacButton(label);
    }

    @Override
    public CheckBox createCheckBox(String label) {
        return new MacCheckBox(label);
    }

    @Override
    public Dialog createDialog(String title) {
        return new MacDialog(title);
    }
}

final class LinuxUIFactory implements UIFactory{

    @Override
    public Button createButton(String label) {
        return new LinuxButton(label);
    }

    @Override
    public CheckBox createCheckBox(String label) {
        return new LinuxCheckBox(label);
    }

    @Override
    public Dialog createDialog(String title) {
        return new LinuxDialog(title);
    }
}

final class UIFactoryProvider {
    private UIFactoryProvider() {}

    public static UIFactory forPlatform(Platform p){
        Objects.requireNonNull(p, "Platform must not be null");

        return switch (p) {
            case Windows -> new WindowsUIFactory();
            case Linux -> new LinuxUIFactory();
            case Mac -> new MacUIFactory();
        };
    }
}



// client interface

final class Application {
    private final UIFactory factory;

    Application(UIFactory factory) {
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
    }

    void renderLoginScreen() {
        Dialog dialog = factory.createDialog("Login");
        dialog.addButton(factory.createButton("Sign in"));
        CheckBox remember = factory.createCheckBox("Remember me");
        remember.toggle();
        dialog.addCheckbox(remember);
        dialog.render();
    }
}

public class AbstractFactory {

    static void main() {
        Application app = new Application(UIFactoryProvider.forPlatform(Platform.Linux));
        app.renderLoginScreen();
    }
}
