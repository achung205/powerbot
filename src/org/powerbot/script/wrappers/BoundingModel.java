package org.powerbot.script.wrappers;

import java.awt.Point;

import org.powerbot.script.methods.MethodContext;
import org.powerbot.script.methods.MethodProvider;
import org.powerbot.script.util.Random;
import org.powerbot.util.math.Vector3;

public abstract class BoundingModel extends MethodProvider {
	private final Vector3 start, end;
	private int[][][] triangles;

	public BoundingModel(final MethodContext ctx, final int x1, final int x2, final int y1, final int y2, final int z1, final int z2) {
		super(ctx);
		start = new Vector3(
				x1 < x2 ? x1 : x2,
				y1 < y2 ? y1 : y2,
				z1 < z2 ? z1 : z2
		);
		end = new Vector3(
				x1 > x2 ? x1 : x2,
				y1 > y2 ? y1 : y2,
				z1 > z2 ? z1 : z2
		);
		triangles = new int[0][][];
		compute();
	}

	public abstract int getX();

	public abstract int getZ();

	public Point getCentroid(final int index) {
		if (index < 0 || index >= triangles.length) {
			return new Point(-1, -1);
		}
		final int x = getX(), z = getZ(), y = ctx.game.tileHeight(x, z, ctx.game.getPlane());
		final Point p = ctx.game.worldToScreen(
				x + (triangles[index][0][0] + triangles[index][1][0] + triangles[index][2][0]) / 3,
				y + (triangles[index][0][1] + triangles[index][1][1] + triangles[index][2][1]) / 3,
				z + (triangles[index][0][2] + triangles[index][1][2] + triangles[index][2][2]) / 3
		);
		return ctx.game.isPointInViewport(p) ? p : new Point(-1, -1);
	}

	public Point getNextPoint() {
		final int faces = triangles.length;
		final int mark = Random.nextInt(0, faces);
		Point point = firstInViewportCentroid(mark, faces);
		return point != null ? point : (point = firstInViewportCentroid(0, mark)) != null ? point : new Point(-1, -1);
	}


	public Point getCenterPoint() {
		final int faces = triangles.length;
		int avgX = 0;
		int avgY = 0;
		int avgZ = 0;
		int index = 0;
		final int x = getX(), z = getZ(), y = ctx.game.tileHeight(x, z, ctx.game.getPlane());
		while (index < faces) {
			avgX += (triangles[index][0][0] + triangles[index][1][0] + triangles[index][2][0]) / 3;
			avgY += (triangles[index][0][1] + triangles[index][1][1] + triangles[index][2][1]) / 3;
			avgZ += (triangles[index][0][2] + triangles[index][1][2] + triangles[index][2][2]) / 3;
			index++;
		}
		final Point p = ctx.game.worldToScreen(
				x + avgX / faces,
				y + avgY / faces,
				z + avgZ / faces
		);
		return ctx.game.isPointInViewport(p) ? p : new Point(-1, -1);
	}

	public boolean contains(final Point p) {
		final int px = p.x, py = p.y;
		final int x = getX(), z = getZ(), y = ctx.game.tileHeight(x, z, ctx.game.getPlane());
		for (final int[][] triangle : triangles) {
			final Point[] arr = {
					ctx.game.worldToScreen(x + triangle[0][0], y + triangle[0][1], z + triangle[0][2]),
					ctx.game.worldToScreen(x + triangle[1][0], y + triangle[1][1], z + triangle[1][2]),
					ctx.game.worldToScreen(x + triangle[2][0], y + triangle[2][1], z + triangle[2][2]),
			};
			if (barycentric(px, py, arr[0].x, arr[0].y, arr[1].x, arr[1].y, arr[2].x, arr[2].y)) {
				return true;
			}
		}
		return false;
	}

	private int firstInViewportIndex(final int pos, final int length) {
		final int x = getX(), z = getZ(), y = ctx.game.tileHeight(x, z, ctx.game.getPlane());
		int index = pos;
		while (index < length) {
			final Point p = ctx.game.worldToScreen(
					x + (triangles[index][0][0] + triangles[index][1][0] + triangles[index][2][0]) / 3,
					y + (triangles[index][0][1] + triangles[index][1][1] + triangles[index][2][1]) / 3,
					z + (triangles[index][0][2] + triangles[index][1][2] + triangles[index][2][2]) / 3
			);
			if (ctx.game.isPointInViewport(p)) {
				return index;
			}
			++index;
		}
		return -1;
	}

	private Point firstInViewportCentroid(final int pos, final int length) {
		final int index = firstInViewportIndex(pos, length);
		return index != -1 ? getCentroid(index) : null;
	}

	private boolean barycentric(final int x, final int y, final int aX, final int aY, final int bX, final int bY, final int cX, final int cY) {
		final int v00 = cX - aX;
		final int v01 = cY - aY;
		final int v10 = bX - aX;
		final int v11 = bY - aY;
		final int v20 = x - aX;
		final int v21 = y - aY;
		final int d00 = v00 * v00 + v01 * v01;
		final int d01 = v00 * v10 + v01 * v11;
		final int d02 = v00 * v20 + v01 * v21;
		final int d11 = v10 * v10 + v11 * v11;
		final int d12 = v10 * v20 + v11 * v21;
		final float denom = 1.0f / (d00 * d11 - d01 * d01);
		final float u = (d11 * d02 - d01 * d12) * denom;
		final float v = (d00 * d12 - d01 * d02) * denom;
		return u >= 0 && v >= 0 && u + v < 1;
	}

	private void compute() {
		if (triangles.length != 0) {
			return;
		}
		final Vector3[] verticies = {
				new Vector3(start.x, start.y, start.z),
				new Vector3(start.x, start.y, end.z),
				new Vector3(end.x, start.y, end.z),
				new Vector3(end.x, start.y, start.z),
				new Vector3(start.x, end.y, start.z),
				new Vector3(start.x, end.y, end.z),
				new Vector3(end.x, end.y, end.z),
				new Vector3(end.x, end.y, start.z),
		};
		final int[][] sides = {
				{0, 1, 2, 3},//BOTTOM
				{4, 5, 6, 7},//TOP
				{1, 5, 6, 2},//FRONT
				{3, 7, 4, 0},//BACK
				{0, 4, 5, 1},//L
				{2, 6, 7, 3},//R
		};
		final int[][] triangles = {
				{0, 1, 3},
				{2, 3, 1},
		};
		final int[][][] model = new int[sides.length * triangles.length][3][3];
		for (int s = 0; s < sides.length; s++) {
			final int[] side = sides[s];
			for (int t = 0; t < triangles.length; t++) {
				final int[] triangle = triangles[t];
				final Vector3 v1 = verticies[side[triangle[0]]], v2 = verticies[side[triangle[1]]], v3 = verticies[side[triangle[2]]];
				final int i = s * 2 + t;
				model[i][0] = v1.toMatrix();
				model[i][1] = v2.toMatrix();
				model[i][2] = v3.toMatrix();
			}
		}
	}
}
