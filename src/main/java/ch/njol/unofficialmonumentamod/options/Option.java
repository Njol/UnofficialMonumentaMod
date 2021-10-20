package ch.njol.unofficialmonumentamod.options;

public class Option<T> {

	final Category category;

	final T defaultValue;

	T value;

	public Option(Category category, T defaultValue) {
		this.category = category;
		this.defaultValue = defaultValue;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	public enum Category {
		MISC, ABILITIES, DEBUG
	}

}
