package org.pcm.automation.api.data.json;

public class JsonTuple<A, B> {
	private A left;
	private B right;

	public JsonTuple(A left, B right) {
		this.left = left;
		this.right = right;
	}

	public JsonTuple() {
	}

	public static <A, B> JsonTuple<A, B> of(final A left, final B right) {
		return new JsonTuple<A, B>(left, right);
	}

	public A getLeft() {
		return left;
	}

	public void setLeft(A left) {
		this.left = left;
	}

	public B getRight() {
		return right;
	}

	public void setRight(B right) {
		this.right = right;
	}
}
